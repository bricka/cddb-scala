package com.bricka.giles.cddb.response

sealed case class CddbReadResponse(discId: String, category: String, discTitle: String, discArtist: String, titles: Map[Int, String])
