package com.bricka.giles.cddb.response

sealed abstract class CddbQueryResponse
case class ExactCddbQueryResponse(category: String, discId: String, discTitle: String) extends CddbQueryResponse
case class InexactCddbQueryResponse(responses: Seq[ExactCddbQueryResponse]) extends CddbQueryResponse
