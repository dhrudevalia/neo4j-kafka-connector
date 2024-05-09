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

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class NodePatternConfigurationTest {

  @Test
  fun `should extract all params`() {
    // given
    val pattern = "(:LabelA:LabelB{!id,*})"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.ALL,
            labels = listOf("LabelA", "LabelB"),
            properties = emptyList(),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all fixed params`() {
    // given
    val pattern = "(:LabelA{!id,foo,bar})"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract complex params`() {
    // given
    val pattern = "(:LabelA{!id,foo.bar})"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo.bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract composite keys with fixed params`() {
    // given
    val pattern = "(:LabelA{!idA,!idB,foo,bar})"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("idA", "idB"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all excluded params`() {
    // given
    val pattern = "(:LabelA{!id,-foo,-bar})"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.EXCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should throw an exception because of mixed configuration`() {
    // given
    val pattern = "(:LabelA{!id,-foo,bar})"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          NodePatternConfiguration.parse(pattern, false)
        }

    assertEquals("The Node pattern $pattern is not homogeneous", exception.message)
  }

  @Test
  fun `should throw an exception because of invalid pattern`() {
    // given
    val pattern = "(LabelA{!id,-foo,bar})"
    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          NodePatternConfiguration.parse(pattern, false)
        }

    assertEquals("The Node pattern $pattern is invalid", exception.message)
  }

  @Test
  fun `should throw an exception because the pattern should contains a key`() {
    // given
    val pattern = "(:LabelA{id,-foo,bar})"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          NodePatternConfiguration.parse(pattern, false)
        }

    assertEquals("The Node pattern $pattern must contain at least one key", exception.message)
  }

  @Test
  fun `should extract all params - simple`() {
    // given
    val pattern = "LabelA:LabelB{!id,*}"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.ALL,
            labels = listOf("LabelA", "LabelB"),
            properties = emptyList(),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all fixed params - simple`() {
    // given
    val pattern = "LabelA{!id,foo,bar}"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract complex params - simple`() {
    // given
    val pattern = "LabelA{!id,foo.bar}"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo.bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract composite keys with fixed params - simple`() {
    // given
    val pattern = "LabelA{!idA,!idB,foo,bar}"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("idA", "idB"),
            type = PatternConfigurationType.INCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all excluded params - simple`() {
    // given
    val pattern = "LabelA{!id,-foo,-bar}"

    // when
    val result = NodePatternConfiguration.parse(pattern, false)

    // then
    val expected =
        NodePatternConfiguration(
            keys = setOf("id"),
            type = PatternConfigurationType.EXCLUDE,
            labels = listOf("LabelA"),
            properties = listOf("foo", "bar"),
            false)
    assertEquals(expected, result)
  }

  @Test
  fun `should throw an exception because of mixed configuration - simple`() {
    // given
    val pattern = "LabelA{!id,-foo,bar}"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          NodePatternConfiguration.parse(pattern, false)
        }

    assertEquals("The Node pattern $pattern is not homogeneous", exception.message)
  }

  @Test
  fun `should throw an exception because the pattern should contains a key - simple`() {
    // given
    val pattern = "LabelA{id,-foo,bar}"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          NodePatternConfiguration.parse(pattern, false)
        }

    assertEquals("The Node pattern $pattern must contain at least one key", exception.message)
  }
}

class RelationshipPatternConfigurationTest {

  @Test
  fun `should extract all params`() {
    // given
    val startPattern = "LabelA{!id,aa}"
    val endPattern = "LabelB{!idB,bb}"
    val pattern = "(:$startPattern)-[:REL_TYPE]->(:$endPattern)"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = NodePatternConfiguration.parse(startPattern, false)
    val end = NodePatternConfiguration.parse(endPattern, false)
    val properties = emptyList<String>()
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.ALL,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all params with reverse source and target`() {
    // given
    val startPattern = "LabelA{!id,aa}"
    val endPattern = "LabelB{!idB,bb}"
    val pattern = "(:$startPattern)<-[:REL_TYPE]-(:$endPattern)"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = NodePatternConfiguration.parse(startPattern, false)
    val end = NodePatternConfiguration.parse(endPattern, false)
    val properties = emptyList<String>()
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = end,
            end = start,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.ALL,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all fixed params`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "(:$startPattern)-[:REL_TYPE{foo, BAR}]->(:$endPattern)"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo", "BAR")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.INCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract complex params`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "(:$startPattern)-[:REL_TYPE{foo.BAR, BAR.foo}]->(:$endPattern)"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo.BAR", "BAR.foo")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.INCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all excluded params`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "(:$startPattern)-[:REL_TYPE{-foo, -BAR}]->(:$endPattern)"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo", "BAR")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.EXCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should throw an exception because of mixed configuration`() {
    // given
    val pattern = "(:LabelA{!id})-[:REL_TYPE{foo, -BAR}]->(:LabelB{!idB})"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          RelationshipPatternConfiguration.parse(pattern, false, false)
        }

    assertEquals("The Relationship pattern $pattern is not homogeneous", exception.message)
  }

  @Test
  fun `should throw an exception because the pattern should contains nodes with only ids`() {
    // given
    val pattern = "(:LabelA{id})-[:REL_TYPE{foo,BAR}]->(:LabelB{!idB})"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          RelationshipPatternConfiguration.parse(pattern, false, false)
        }

    assertEquals("The Relationship pattern $pattern is invalid", exception.message)
  }

  @Test
  fun `should throw an exception because the pattern is invalid`() {
    // given
    val pattern = "(LabelA{!id})-[:REL_TYPE{foo,BAR}]->(:LabelB{!idB})"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          RelationshipPatternConfiguration.parse(pattern, false, false)
        }

    assertEquals("The Relationship pattern $pattern is invalid", exception.message)
  }

  @Test
  fun `should extract all params - simple`() {
    // given
    val startPattern = "LabelA{!id,aa}"
    val endPattern = "LabelB{!idB,bb}"
    val pattern = "$startPattern REL_TYPE $endPattern"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = NodePatternConfiguration.parse(startPattern, false)
    val end = NodePatternConfiguration.parse(endPattern, false)
    val properties = emptyList<String>()
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.ALL,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all fixed params - simple`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "$startPattern REL_TYPE{foo, BAR} $endPattern"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo", "BAR")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.INCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract complex params - simple`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "$startPattern REL_TYPE{foo.BAR, BAR.foo} $endPattern"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo.BAR", "BAR.foo")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.INCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should extract all excluded params - simple`() {
    // given
    val startPattern = "LabelA{!id}"
    val endPattern = "LabelB{!idB}"
    val pattern = "$startPattern REL_TYPE{-foo, -BAR} $endPattern"

    // when
    val result = RelationshipPatternConfiguration.parse(pattern, false, false)

    // then
    val start = RelationshipPatternConfiguration.getNodeConf(startPattern, false)
    val end = RelationshipPatternConfiguration.getNodeConf(endPattern, false)
    val properties = listOf("foo", "BAR")
    val relType = "REL_TYPE"
    val expected =
        RelationshipPatternConfiguration(
            start = start,
            end = end,
            relType = relType,
            properties = properties,
            type = PatternConfigurationType.EXCLUDE,
            mergeProperties = false)
    assertEquals(expected, result)
  }

  @Test
  fun `should throw an exception because of mixed configuration - simple`() {
    // given
    val pattern = "LabelA{!id} REL_TYPE{foo, -BAR} LabelB{!idB}"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          RelationshipPatternConfiguration.parse(pattern, false, false)
        }

    assertEquals("The Relationship pattern $pattern is not homogeneous", exception.message)
  }

  @Test
  fun `should throw an exception because the pattern should contains nodes with only ids - simple`() {
    // given
    val pattern = "LabelA{id} REL_TYPE{foo,BAR} LabelB{!idB}"

    val exception =
        assertFailsWith(IllegalArgumentException::class) {
          RelationshipPatternConfiguration.parse(pattern, false, false)
        }

    assertEquals("The Relationship pattern $pattern is invalid", exception.message)
  }
}
