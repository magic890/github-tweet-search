package com.matteoguarnerio.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class SearchResults(
                          searchQuery: String,
                          searchResults: Seq[SearchResult]
                        )

object SearchResults {
  implicit val searchResultsDecoder: Decoder[SearchResults] = deriveDecoder[SearchResults]
  implicit val searchResultsEncoder: Encoder[SearchResults] = deriveEncoder[SearchResults]
}
