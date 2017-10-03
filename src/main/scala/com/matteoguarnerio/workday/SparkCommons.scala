package com.matteoguarnerio.workday

import java.io.{File, PrintWriter}

import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf

import scala.io.Source

object SparkCommons {
  lazy val driverPort = 7777
  lazy val driverHost = "localhost"

  lazy val conf: SparkConf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("Workday - GRID Assignment")
    .set("spark.driver.port", driverPort.toString)
    .set("spark.driver.host", driverHost)

  lazy val ss: SparkSession = SparkSession.builder
    .config(conf = conf)
    .getOrCreate()

  /**
    * Load twitter oauth keys for twitter4j client from twitter4j.properties
    * It should contain the following keys:
    *
    * twitter4j.oauth.consumerKey=..
    * twitter4j.oauth.consumerSecret=..
    * twitter4j.oauth.accessToken=..
    * twitter4j.oauth.accessTokenSecret=..
    */
  def loadTwitterKeys(): Unit = {
    val lines: Iterator[String] = Source.fromFile("resources/twitter4j.properties").getLines()
    val props = lines.map(line => line.split("=")).map { case (scala.Array(k, v)) => (k, v)}
    props.foreach {
      case (k: String, v: String) => System.setProperty(k.trim, v.trim)
    }
  }

  def writeToFile(file: String, content: String): Unit = {
    val writer = new PrintWriter(new File(file))
    writer.write(content)
    writer.close()
  }

}
