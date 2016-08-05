package com.bricka.giles.cddb

import scala.concurrent.{ExecutionContext, Future}

trait CddbClient {
  def query(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): Future[Option[CddbClient.response.CddbQueryResponse]]
  def read(category: String, discId: String): Future[Option[CddbClient.response.CddbReadResponse]]
}

object CddbClient {
  case class CddbException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

  object response {
    sealed abstract class CddbQueryResponse
    case class ExactCddbQueryResponse(category: String, discId: String, discTitle: String) extends CddbQueryResponse
    case class InexactCddbQueryResponse(responses: Seq[ExactCddbQueryResponse]) extends CddbQueryResponse

    sealed case class CddbReadResponse(discId: String, category: String, discTitle: String, discArtist: String, titles: Traversable[String])
  }
}
