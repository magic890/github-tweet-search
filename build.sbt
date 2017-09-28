name         := "workday-assignment"

organization := "com.matteoguarnerio"

version      := "0.0.1"

scalaVersion := "2.11.11"

lazy val sparkVersion = "2.2.0"

libraryDependencies ++= Seq(
  "org.apache.spark"            %%  "spark-core"                  % sparkVersion,
  "org.apache.spark"            %%  "spark-streaming"             % sparkVersion,
  "org.apache.spark"            %%  "spark-sql"                   % sparkVersion,
  "org.apache.bahir"            %%  "spark-streaming-twitter"     % sparkVersion,
  "org.twitter4j" % "twitter4j-core" % "4.0.6"
)
