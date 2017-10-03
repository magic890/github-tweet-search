package com.matteoguarnerio.workday.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto._

case class Repo(
               name: String,
               url: String,
               description: String
               )

object Repo {
  implicit val repoDecoder: Decoder[Repo] = deriveDecoder[Repo]

  implicit val repoEncoder: Encoder[Repo] = new Encoder[Repo] {
    final def apply(r: Repo): Json = {
      val desc: String = if (r.description == null || r.description.trim.isEmpty) "" else r.description
      Json.obj(
        ("name", Json.fromString(r.name)),
        ("url", Json.fromString(r.url)),
        ("description", Json.fromString(desc))
      )
    }
  }
}
