package com.matteoguarnerio.workday.spark

import java.util

import com.matteoguarnerio.workday.SparkCommons
import com.matteoguarnerio.workday.model.{Repo, SearchResult, SearchResults, Tweet, User}
import io.circe.syntax._
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import twitter4j.{Query, Status, TwitterException, TwitterFactory}

import scala.collection.{JavaConverters, immutable}
import scala.concurrent.{Await, Future}

object SparkOperations extends App {

  import SparkCommons.ss.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  private def searchGitHubRepos(searchStr: String): Array[Repo] = {

    def apiPaginationCall(page: Int = 1): Future[HttpResponse] = Future {
      val connectionTimeoutMs: Int = 20000

      val requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(connectionTimeoutMs)
        .setConnectTimeout(connectionTimeoutMs)
        .setSocketTimeout(connectionTimeoutMs)
        .build()
      HttpClientBuilder.create().build()

      val client: CloseableHttpClient = HttpClientBuilder.create().build()

      val req = new HttpGet(s"https://api.github.com/search/repositories?q=$searchStr&page=$page")
      req.setConfig(requestConfig)

      client.execute(req)
    }

    val firstResponse: HttpResponse = Await.result(apiPaginationCall(1), scala.concurrent.duration.Duration.Inf)

    val headers: Map[String, String] = firstResponse.getAllHeaders.map(h => h.getName -> h.getValue).toMap
    val numRequestToPerform: Int = Math.min(headers.getOrElse("X-RateLimit-Limit", "10").toInt, headers.getOrElse("X-RateLimit-Remaining", "10").toInt)
    val otherResponses: Seq[Future[HttpResponse]] = 2 until numRequestToPerform + 1 map(i => {
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

    val res: Array[Repo] = repoRDD
      .collect()
      .flatten
      .map {
        case (fn, hu, d) => Repo(fn, hu, d)
      }

    println(s"Read ${numRequestToPerform + 1} GitHub paginated API responses for '$searchStr'")

    res
  }

  private def searchTwitter(searchStr: String): Future[Seq[Status]] = Future {
    val twitter = TwitterFactory.getSingleton

    val res: Option[util.List[Status]] =
      try {
        val tweets: util.List[Status] = twitter.search(new Query(searchStr)).getTweets
        println(s"Found ${tweets.size()} tweets for '$searchStr'")
        Option(tweets)
      }
      catch {
        case e: TwitterException => {
          println(s"Unable to search tweets for '$searchStr'")
          System.err.println(e)
          None
        }
      }

    val ret: Seq[Status] = res match {
      case Some(r) => JavaConverters.collectionAsScalaIterableConverter(r).asScala.toSeq
      case None => Seq.empty
    }

    ret
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
    val resultJsonStr: String = result.asJson.spaces2

    val savingPath: String = s"output/${System.currentTimeMillis()}-$searchQuery.json"

    println("Result ---------> "+ savingPath)
    println(resultJsonStr)
    println("EOF ---------")

    SparkCommons.writeToFile(savingPath, resultJsonStr)
  }

  override def main(args: Array[String]): Unit = {
    Logger.getRootLogger.setLevel(Level.ERROR)
    SparkCommons.loadTwitterKeys()

    val searchQuery: String = args.foldLeft("")((acc, arg) => acc + " " + arg).trim

    startStream(searchQuery)

    SparkCommons.ss.close()
  }

}
