package com.matteoguarnerio.workday.model

import io.circe._, io.circe.generic.semiauto._

case class User(
               id: Long,
               name: String,
               screenName: String
               )

case class Tweet(
                  createdAt: String,
                  id: Long,
                  text: String,
                  isRetweet: Boolean,
                  username: User,
                  lang: String
                )

object User {
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
}

object Tweet {
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
  implicit val tweetEncoder: Encoder[Tweet] = deriveEncoder[Tweet]
}
