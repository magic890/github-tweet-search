package com.matteoguarnerio.workday.spark

import java.io.{File, PrintWriter}
import java.util

import com.matteoguarnerio.workday.SparkCommons
//import com.matteoguarnerio.workday.SparkCommons.ssc
import com.matteoguarnerio.workday.model.{Repo, SearchResult, SearchResults, Tweet, User}
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DataTypes, StructField}
import twitter4j.{Query, Status, TwitterFactory}

import scala.collection.{JavaConverters, immutable}
import scala.concurrent.{Await, Future}
import scala.collection.JavaConversions._
import io.circe._
import io.circe.syntax._

import scala.io.Source


object SparkOperations extends App {

  import scala.concurrent.ExecutionContext.Implicits.global
  import SparkCommons.ss.implicits._

  private val CONNECTION_TIMEOUT_MS: Int = 20000; // Timeout in millis (20 sec).

  private def searchGitHubRepos(searchStr: String): Array[Repo] = {

    // TODO: check searchStr encode with string with spaces

    def apiPaginationCall(page: Int = 1): Future[HttpResponse] = Future {
      val requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
        .setConnectTimeout(CONNECTION_TIMEOUT_MS)
        .setSocketTimeout(CONNECTION_TIMEOUT_MS)
        .build()
      HttpClientBuilder.create().build()

      val client: CloseableHttpClient = HttpClientBuilder.create().build()

      val req = new HttpGet(s"https://api.github.com/search/repositories?q=$searchStr&page=$page")
      req.setConfig(requestConfig)

      client.execute(req)
    }

    val firstResponse: HttpResponse = Await.result(apiPaginationCall(1), scala.concurrent.duration.Duration.Inf)

    // TODO: fix call counter
    val headers: Map[String, String] = firstResponse.getAllHeaders.map(h => h.getName -> h.getValue).toMap
    val otherResponses: Seq[Future[HttpResponse]] = 2 until 3 map(i => { //headers.getOrElse("X-RateLimit-Limit", "10").toInt
      apiPaginationCall(i)
    })

    val responses: Seq[HttpResponse] = firstResponse +: otherResponses.map(res => Await.result(res, scala.concurrent.duration.Duration.Inf))
    val responsesStr: Seq[String] = responses
      .map(res => scala.io.Source.fromInputStream(res.getEntity.getContent).mkString)
    val repoDF: DataFrame = SparkCommons.ss.read.format("json").json(
      SparkCommons.ss.createDataset(responsesStr)
    )

    val repoRDD: RDD[Seq[(String, String, String)]] = repoDF
      .select("items.full_name", "items.html_url", "items.description")
      .rdd
      .map(row => {
        val fnL: Seq[String] = row.getSeq[String](0)
        val huL: Seq[String] = row.getSeq[String](1)
        val dL: Seq[String] = row.getSeq[String](2)
        (fnL, huL, dL).zipped.toSeq
      })

    val r: Array[(String, String, String)] = repoRDD
      .collect()
      .flatten

    val t = r.map {
      case (fn, hu, d) => Repo(fn, hu, d)
    }

    t
  }

  private def searchTwitter(searchStr: String): Future[Seq[Status]] = Future {
    val twitter = TwitterFactory.getSingleton

    val res: Seq[Status] = JavaConverters.collectionAsScalaIterableConverter(
      twitter.search(new Query(searchStr)).getTweets
    ).asScala.toSeq

    res
  }

  private def startStream(searchQuery: String): Unit = {
    val repositories = searchGitHubRepos(searchQuery)

    val twitterSearches: immutable.IndexedSeq[(Repo, Future[Seq[Status]])] = repositories.indices map(i => {
      (repositories(i), searchTwitter(repositories(i).name))
    })

    val searchResults: immutable.IndexedSeq[SearchResult] = twitterSearches
      .map { case (repo, tweets) =>
        val ts = Await.result(tweets, scala.concurrent.duration.Duration.Inf)
        val l = ts.map(t => Tweet(t.getCreatedAt.toString, t.getId, t.getText, t.isRetweet, User(t.getUser.getId, t.getUser.getName, t.getUser.getScreenName), t.getLang))
        SearchResult(repo, l)
      }

    val result: SearchResults = SearchResults(searchQuery, searchResults)
    val resultJson: Json = result.asJson

    println(resultJson.spaces2)

    SparkCommons.writeToFile(s"output/${System.currentTimeMillis()}-$searchQuery.json", resultJson.noSpaces)
  }

  override def main(args: Array[String]): Unit = {
    Logger.getRootLogger.setLevel(Level.ERROR)
    SparkCommons.loadTwitterKeys()

    val searchQuery: String = args.foldLeft("")((acc, arg) => acc + " " + arg).trim

    startStream(searchQuery)
  }

}
