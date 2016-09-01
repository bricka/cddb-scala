package com.bricka.giles.cddb

import scala.concurrent.{ExecutionContext, Future}

import com.bricka.giles.cddb.response.{CddbQueryResponse, CddbReadResponse}

trait CddbClient {
  def query(discId: String, trackOffsets: Seq[Long], numSeconds: Int): Future[Option[CddbQueryResponse]]
  def read(category: String, discId: String): Future[Option[CddbReadResponse]]
}
