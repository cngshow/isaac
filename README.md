ISAAC
======================

ISAAC Object Chronicle Project

mvn clean deploy -DaltDeploymentRepository=vadev::default::https://vadev.mantech.com:8080/nexus/content/repositories/snapshots

Release Notes
mvn jgitflow:release-start jgitflow:release-finish -DreleaseVersion=3.01 -DdevelopmentVersion=3.02-SNAPSHOT

