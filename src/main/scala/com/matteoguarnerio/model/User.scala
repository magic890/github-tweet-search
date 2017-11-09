package com.matteoguarnerio.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class User(
                 id: Long,
                 name: String,
                 screenName: String
               )

object User {
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
}
