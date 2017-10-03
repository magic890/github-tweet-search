package com.matteoguarnerio.workday.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class SearchResult(
                         repo: Repo,
                         tweets: Seq[Tweet]
                       )

object SearchResult {
  implicit val searchResultDecoder: Decoder[SearchResult] = deriveDecoder[SearchResult]
  implicit val searchResultEncoder: Encoder[SearchResult] = deriveEncoder[SearchResult]
}
