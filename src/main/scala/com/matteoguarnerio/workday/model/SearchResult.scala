package com.matteoguarnerio.workday.model

import io.circe._, io.circe.generic.semiauto._

case class SearchResult(repo: Repo, tweets: Seq[Tweet])

case class SearchResults(searchQuery: String, searchResults: Seq[SearchResult])

object SearchResult {
  implicit val searchResultDecoder: Decoder[SearchResult] = deriveDecoder[SearchResult]
  implicit val searchResultEncoder: Encoder[SearchResult] = deriveEncoder[SearchResult]
}

object SearchResults {
  implicit val searchResultsDecoder: Decoder[SearchResults] = deriveDecoder[SearchResults]
  implicit val searchResultsEncoder: Encoder[SearchResults] = deriveEncoder[SearchResults]
}
