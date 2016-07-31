package com.bricka.giles.cddb

trait CddbClient {
  def getInfo(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): Option[CddbInfo]
}
