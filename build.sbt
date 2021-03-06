resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns))

addSbtPlugin("com.typesafe" % "sbt-native-packager" % "0.4.4")

addSbtPlugin("play" % "sbt-plugin" % "2.1.1")

sbtPlugin := true

name := "play2-ubuntu-package"

organization := "com.lunatech"

version := "0.5.3"

description := "Play 2 plugin for building Ubuntu packages"

publishMavenStyle := false

publishArtifact in Test := false

publishTo := Some(Resolver.url("Rhinofly Internal release Repository", new URL("http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"))(Resolver.ivyStylePatterns))
//Some(Resolver.url("sbt-plugins-public", new URL("http://artifactory.lunatech.com/artifactory/sbt-plugins-public/"))(Resolver.ivyStylePatterns))

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

scalaVersion := "2.9.2"
