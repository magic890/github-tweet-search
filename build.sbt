name         := "github-tweet-search"

organization := "com.matteoguarnerio"

version      := "0.0.1"

scalaVersion := "2.11.11"

lazy val sparkVersion       = "2.2.0"
lazy val twitter4jVersion   = "4.0.6"
lazy val circeVersion       = "0.8.0"

libraryDependencies ++= Seq(
  "org.apache.spark"            %%  "spark-core"                  % sparkVersion,
  "org.apache.spark"            %%  "spark-sql"                   % sparkVersion,
  "org.twitter4j"               %   "twitter4j-core"              % twitter4jVersion,
  "io.circe"                    %%  "circe-core"                  % circeVersion,
  "io.circe"                    %%  "circe-generic"               % circeVersion
)
