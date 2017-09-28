package com.matteoguarnerio.workday.spark

import com.matteoguarnerio.workday.SparkCommons
import com.matteoguarnerio.workday.SparkCommons.ssc
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Duration, Seconds}
import twitter4j.{Query, Status}
import twitter4j.api.SearchResource

import scala.concurrent.{Await, Future}

object SparkOperations extends App {

  import scala.concurrent.ExecutionContext.Implicits.global
  import SparkCommons.ss.implicits._

  private val CONNECTION_TIMEOUT_MS: Int = 20000; // Timeout in millis (20 sec).

  private def searchGitHubRepos(searchStr: String): Array[String] = {

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

    val repoFullNameRDD: RDD[Seq[String]] = repoDF
      .select("items.full_name")
      .rdd
      .map(row  => {
          val a = row

        val z: Seq[String] = row.getSeq[String](0)

        z
          //val afullname = row.getAs[List[String]]("full_name")
         // row

        }
      )

//    val d: Array[Seq[String]] = repoFullNameRDD.collect()
//    val dm: Array[String] = d.flatten


    // TODO: remove assignment and return
    val test = repoFullNameRDD.collect().flatten
    test
  }


  private def startStream(): Unit = {
    // Github API Search
    // for each `items` search for the `full_name` (e.g. component/reactive) that is the suffix of `html_url`
    // in the response there is `total_count`

    val repositories: Array[String] = searchGitHubRepos("reactive")

    import twitter4j.QueryResult
    import twitter4j.Twitter
    import twitter4j.TwitterFactory
    // The factory instance is re-useable and thread safe.// The factory instance is re-useable and thread safe.

    val twitter = TwitterFactory.getSingleton

    val q = Query("component/reactive")

    val a = twitter.search(SearchResource(QueryResult("ciao")))

    val query = twitter.search("source:twitter4j yusukey")
    val result = twitter.search(query)
    import scala.collection.JavaConversions._
    for (status <- result.getTweets) {
      System.out.println("@" + status.getUser.getScreenName + ":" + status.getText)
    }


    val tweets: ReceiverInputDStream[Status] = TwitterUtils.createStream(SparkCommons.ssc, None, repositories)

    val duration: Duration = Seconds(3600)

    // Print tweets batch count
    tweets.foreachRDD(rdd => {
//      val s = rdd.asInstanceOf[Status]
//      println(s"${s.getCreatedAt} - ${s.getId}, ${s.getUser.getId}, ${s.getUser.getName}, ${s.getUser.getScreenName} - ${s.getText}")
      println("\nNew tweets %s:".format(rdd.count()))
    })

    tweets.map(
      s => println(s"${s.getCreatedAt} - ${s.getId}, ${s.getUser.getId}, ${s.getUser.getName}, ${s.getUser.getScreenName} - ${s.getText}")
    )



    //t.getCreatedAt, t.getId, t.getUser.getId, t.getUser.getName, t.getUser.getScreenName, t.getText

    // Get users and followers count
//    val users: DStream[(String, Int)] = tweets.map(status =>
//      (status.getUser.getScreenName, status.getUser.getFollowersCount)
//    )
//
//
//    // Print top users
//    val usersReduced = users.reduceByKeyAndWindow(_ + _, duration).map { case (user, count) => (count, user) }.transform(_.sortByKey(false))
//    usersReduced.foreachRDD(rdd => {
//      println("ReducedUsersCount= %s ".format(rdd.count()))
//      val topUsers = rdd.take(10)
//      topUsers.foreach { case (count, user) => println("%s (%s followers)".format(user, count)) }
//    })
//
//    // Print popular hash tags
//    val hashTags = tweets.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))
//    val topHashTags = hashTags.map((_, 1))
//      .reduceByKeyAndWindow(_ + _, duration)
//      .map { case (topic, count) => (count, topic) }
//      .transform(_.sortByKey(false))
//
//    topHashTags.foreachRDD(rdd => {
//      val topList = rdd.take(10)
//      println("\nPopular topics in last %s seconds (%s total):".format(duration, rdd.count()))
//      topList.foreach { case (count, tag) => println("%s (%s tweets)".format(tag, count)) }
//    })

    ssc.start()
    //ssc.awaitTermination(Minutes(300).milliseconds)
    ssc.awaitTermination()
    ssc.stop(true)

    //  if (sc != null) {
    //    sc.stop()
    //  }

    //  df.write.mode('append').json(yourtargetpath)
  }

  override def main(args: Array[String]): Unit = {
    Logger.getRootLogger.setLevel(Level.ERROR)
    SparkCommons.loadTwitterKeys()
    startStream()
    //searchGitHubRepos("reactive")
  }
}
