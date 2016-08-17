case class CddbException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)
