package com.matteoguarnerio.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto._

case class Tweet(
                  createdAt: String,
                  id: Long,
                  text: String,
                  isRetweet: Boolean,
                  username: User,
                  lang: String
                ) {
  val url: String = s"https://twitter.com/${username.screenName}/status/$id"
}

object Tweet {
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]

  implicit val tweetEncoder: Encoder[Tweet] = new Encoder[Tweet] {
    final def apply(t: Tweet): Json = {
      Json.obj(
        ("createdAt", Json.fromString(t.createdAt)),
        ("id", Json.fromLong(t.id)),
        ("text", Json.fromString(t.text)),
        ("isRetweet", Json.fromBoolean(t.isRetweet)),
        ("username", User.userEncoder(t.username)),
        ("lang", Json.fromString(t.lang)),
        ("url", Json.fromString(t.url))
      )
    }
  }
}
