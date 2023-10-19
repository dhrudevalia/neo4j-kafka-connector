import builds.Build
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.version

version = "2023.05"

project {
  params {
    password("github-commit-status-token", "credentialsJSON:be6ac011-ba27-4f2e-a628-edce6708504e")
    password("github-pull-request-token", "credentialsJSON:be6ac011-ba27-4f2e-a628-edce6708504e")
    text("github-packages-user", "neo4j-build-service")
    password("github-packages-token", "credentialsJSON:d5ea2df2-7a81-41d4-98bf-cb7ebd607493")
  }

  vcsRoot(
      GitVcsRoot {
        id("Connectors_Neo4jKafkaConnector_VCS")

        name = "git@github.com:neo4j/neo4j-kafka-connector.git"
        url = "git@github.com:neo4j/neo4j-kafka-connector.git"
        branch = "refs/heads/main"
        branchSpec = "refs/heads/*"
      })

  subProject(
      Build(
          "main",
          """
                +:main
            """
              .trimIndent(),
          false))
  subProject(
      Build(
          "pull-request",
          """
                +:pull/*
            """
              .trimIndent(),
          true))
}
