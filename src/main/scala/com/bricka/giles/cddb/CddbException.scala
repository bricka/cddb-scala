package com.bricka.giles.cddb

case class CddbException(message: String, cause: Throwable = null) extends RuntimeException(message, cause) // scalastyle:off null
