/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.connectors.kafka.service.sink.strategy

import org.neo4j.connectors.kafka.events.Constraint
import org.neo4j.connectors.kafka.events.EntityType
import org.neo4j.connectors.kafka.events.NodeChange
import org.neo4j.connectors.kafka.events.OperationType
import org.neo4j.connectors.kafka.events.RelationshipPayload
import org.neo4j.connectors.kafka.events.StreamsConstraintType
import org.neo4j.connectors.kafka.events.StreamsTransactionEvent
import org.neo4j.connectors.kafka.extensions.quote
import org.neo4j.connectors.kafka.service.StreamsSinkEntity
import org.neo4j.connectors.kafka.utils.IngestionUtils.getLabelsAsString
import org.neo4j.connectors.kafka.utils.IngestionUtils.getNodeKeysAsString
import org.neo4j.connectors.kafka.utils.SchemaUtils.getNodeKeys
import org.neo4j.connectors.kafka.utils.SchemaUtils.toStreamsTransactionEvent
import org.neo4j.connectors.kafka.utils.StreamsUtils

class SchemaIngestionStrategy : IngestionStrategy {

  private fun prepareRelationshipEvents(
      events: List<StreamsTransactionEvent>,
      withProperties: Boolean = true
  ): Map<RelationshipSchemaMetadata, List<Map<String, Any>>> =
      events
          .mapNotNull {
            val payload = it.payload as RelationshipPayload

            val startNodeConstraints =
                getNodeConstraints(it) {
                  it.type == StreamsConstraintType.UNIQUE &&
                      payload.start.labels.orEmpty().contains(it.label)
                }
            val endNodeConstraints =
                getNodeConstraints(it) {
                  it.type == StreamsConstraintType.UNIQUE &&
                      payload.end.labels.orEmpty().contains(it.label)
                }

            if (constraintsAreEmpty(startNodeConstraints, endNodeConstraints)) {
              null
            } else {
              createRelationshipMetadata(
                  payload, startNodeConstraints, endNodeConstraints, withProperties)
            }
          }
          .groupBy { it.first }
          .mapValues { it.value.map { it.second } }

  private fun createRelationshipMetadata(
      payload: RelationshipPayload,
      startNodeConstraints: List<Constraint>,
      endNodeConstraints: List<Constraint>,
      withProperties: Boolean
  ): Pair<RelationshipSchemaMetadata, Map<String, Map<String, Any>>>? {
    val startNodeKeys =
        getNodeKeys(
            labels = payload.start.labels.orEmpty(),
            propertyKeys = payload.start.ids.keys,
            constraints = startNodeConstraints)
    val endNodeKeys =
        getNodeKeys(
            labels = payload.end.labels.orEmpty(),
            propertyKeys = payload.end.ids.keys,
            constraints = endNodeConstraints)
    val start = payload.start.ids.filterKeys { startNodeKeys.contains(it) }
    val end = payload.end.ids.filterKeys { endNodeKeys.contains(it) }

    return if (idsAreEmpty(start, end)) {
      null
    } else {
      val value =
          if (withProperties) {
            val properties = payload.after?.properties ?: payload.before?.properties ?: emptyMap()
            mapOf("start" to start, "end" to end, "properties" to properties)
          } else {
            mapOf("start" to start, "end" to end)
          }
      val key =
          RelationshipSchemaMetadata(
              label = payload.label,
              startLabels =
                  payload.start.labels.orEmpty().filter { label ->
                    startNodeConstraints.any { it.label == label }
                  },
              endLabels =
                  payload.end.labels.orEmpty().filter { label ->
                    endNodeConstraints.any { it.label == label }
                  },
              startKeys = start.keys,
              endKeys = end.keys)
      key to value
    }
  }

  private fun idsAreEmpty(start: Map<String, Any>, end: Map<String, Any>) =
      start.isEmpty() || end.isEmpty()

  private fun constraintsAreEmpty(
      startNodeConstraints: List<Constraint>,
      endNodeConstraints: List<Constraint>
  ) = startNodeConstraints.isEmpty() || endNodeConstraints.isEmpty()

  override fun mergeRelationshipEvents(events: Collection<StreamsSinkEntity>): List<QueryEvents> {
    return prepareRelationshipEvents(
            events.mapNotNull {
              toStreamsTransactionEvent(it) {
                it.payload.type == EntityType.relationship &&
                    it.meta.operation != OperationType.deleted
              }
            })
        .map {
          val label = it.key.label.quote()
          val query =
              """
                        |${StreamsUtils.UNWIND}
                        |MERGE (start${getLabelsAsString(it.key.startLabels)}{${getNodeKeysAsString("start", it.key.startKeys)}})
                        |MERGE (end${getLabelsAsString(it.key.endLabels)}{${getNodeKeysAsString("end", it.key.endKeys)}})
                        |MERGE (start)-[r:$label]->(end)
                        |SET r = event.properties
                    """
                  .trimMargin()
          QueryEvents(query, it.value)
        }
  }

  override fun deleteRelationshipEvents(events: Collection<StreamsSinkEntity>): List<QueryEvents> {
    return prepareRelationshipEvents(
            events.mapNotNull {
              toStreamsTransactionEvent(it) {
                it.payload.type == EntityType.relationship &&
                    it.meta.operation == OperationType.deleted
              }
            },
            false)
        .map {
          val label = it.key.label.quote()
          val query =
              """
                        |${StreamsUtils.UNWIND}
                        |MATCH (start${getLabelsAsString(it.key.startLabels)}{${getNodeKeysAsString("start", it.key.startKeys)}})
                        |MATCH (end${getLabelsAsString(it.key.endLabels)}{${getNodeKeysAsString("end", it.key.endKeys)}})
                        |MATCH (start)-[r:$label]->(end)
                        |DELETE r
                    """
                  .trimMargin()
          QueryEvents(query, it.value)
        }
  }

  override fun deleteNodeEvents(events: Collection<StreamsSinkEntity>): List<QueryEvents> {
    return events
        .mapNotNull {
          toStreamsTransactionEvent(it) {
            it.payload.type == EntityType.node && it.meta.operation == OperationType.deleted
          }
        }
        .mapNotNull {
          val changeEvtBefore = it.payload.before as NodeChange
          val constraints = getNodeConstraints(it) { it.type == StreamsConstraintType.UNIQUE }
          if (constraints.isEmpty()) {
            null
          } else {
            constraints to mapOf("properties" to changeEvtBefore.properties)
          }
        }
        .groupBy({ it.first }, { it.second })
        .map {
          val labels = it.key.mapNotNull { it.label }
          val nodeKeys = it.key.flatMap { it.properties }.toSet()
          val query =
              """
                        |${StreamsUtils.UNWIND}
                        |MATCH (n${getLabelsAsString(labels)}{${getNodeKeysAsString(keys = nodeKeys)}})
                        |DETACH DELETE n
                    """
                  .trimMargin()
          QueryEvents(query, it.value)
        }
  }

  override fun mergeNodeEvents(events: Collection<StreamsSinkEntity>): List<QueryEvents> {
    val filterLabels: (List<String>, List<Constraint>) -> List<String> = { labels, constraints ->
      labels
          .filter { label -> !constraints.any { constraint -> constraint.label == label } }
          .map { it.quote() }
    }
    return events
        .mapNotNull {
          toStreamsTransactionEvent(it) {
            it.payload.type == EntityType.node && it.meta.operation != OperationType.deleted
          }
        }
        .mapNotNull {
          val changeEvtAfter = it.payload.after as NodeChange
          val labelsAfter = changeEvtAfter.labels ?: emptyList()
          val labelsBefore = (it.payload.before as? NodeChange)?.labels.orEmpty()

          val constraints = getNodeConstraints(it) { it.type == StreamsConstraintType.UNIQUE }
          if (constraints.isEmpty()) {
            null
          } else {
            val labelsToAdd = filterLabels((labelsAfter - labelsBefore), constraints)
            val labelsToDelete = filterLabels((labelsBefore - labelsAfter), constraints)

            val propertyKeys = changeEvtAfter.properties?.keys ?: emptySet()
            val keys = getNodeKeys(labelsAfter, propertyKeys, constraints)

            if (keys.isEmpty()) {
              null
            } else {
              val key =
                  NodeSchemaMetadata(
                      constraints = constraints,
                      labelsToAdd = labelsToAdd,
                      labelsToDelete = labelsToDelete,
                      keys = keys)
              val value = mapOf("properties" to changeEvtAfter.properties)
              key to value
            }
          }
        }
        .groupBy({ it.first }, { it.second })
        .map { map ->
          var query =
              """
                        |${StreamsUtils.UNWIND}
                        |MERGE (n${getLabelsAsString(map.key.constraints.mapNotNull { it.label })}{${getNodeKeysAsString(keys = map.key.keys)}})
                        |SET n = event.properties
                    """
                  .trimMargin()
          if (map.key.labelsToAdd.isNotEmpty()) {
            query += "\nSET n${getLabelsAsString(map.key.labelsToAdd)}"
          }
          if (map.key.labelsToDelete.isNotEmpty()) {
            query += "\nREMOVE n${getLabelsAsString(map.key.labelsToDelete)}"
          }
          QueryEvents(query, map.value)
        }
  }

  private fun getNodeConstraints(
      event: StreamsTransactionEvent,
      filter: (Constraint) -> Boolean
  ): List<Constraint> = event.schema.constraints.filter { filter(it) }
}
