package com.bricka.giles.cddb

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import scalaj.http.{Http, HttpResponse}

class CddbClientImpl(cddbHttpPath: String)(implicit ec: ExecutionContext) extends CddbClient {
  import CddbClient._
  import CddbClient.response._
  import CddbClientImpl._

  override def query(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): Future[Option[CddbQueryResponse]] =
    responseWithCmd(s"cddb+query+${discId}+${numTracks}+${trackOffsets.mkString("+")}+${numSeconds}").map { queryResponse =>
      if (queryResponse.isError) {
        throw CddbException(s"Could not query CDDB: received error ${queryResponse.code}, body: ${queryResponse.body}")
      }

      val bodyLines = queryResponse.body.split("\n")

      val firstLine = bodyLines.head
      val code = firstLine.split(" ").head

      code match {
        case "200" => Some(exactCddbQueryResponseFromLine(firstLine.split(" ").drop(1)))
        case "211" => Some(inexactCddbQueryResponseFromLines(bodyLines.drop(1)))
        case "202" => None
        case "403" => throw CddbException("CDDB Database Corrupted")
        case "409" => throw CddbException("Could not handshake with CDDB server")
        case _ => throw CddbException(s"Unknown CDDB error code: ${code}")
      }
    }

  private def exactCddbQueryResponseFromLine(components: Array[String]): ExactCddbQueryResponse = components match {
    case Array(categ, discId, title) => ExactCddbQueryResponse(category = categ,
                                                               discId = discId,
                                                               discTitle = title)
  }

  private def inexactCddbQueryResponseFromLines(lines: Array[String]): InexactCddbQueryResponse =
    InexactCddbQueryResponse(lines.map(_.split(" ")).map(exactCddbQueryResponseFromLine))

  override def read(category: String, discId: String): Future[Option[CddbReadResponse]] = Future {
    val readResponse = responseWithCmd(s"cddb+read+${category}+${discId}")

    if (readResponse.isError) {
      throw CddbException(s"Could not run read command on CDDB: received error ${readResponse.code}, body: ${readResponse.body}")
    }

    val bodyLines = readResponse.body.split("\n")

    val firstLine = bodyLines.head
    val code = firstLine.split(" ").head

    code match {
      case "210" => Some(readResponseFromBodyLines(bodyLines))
      case "401" => None
      case "402" => throw CddbException("Encountered server error when running read command on CDDB")
      case "403" => throw CddbException("CDDB Database Corrupted")
      case "409" => throw CddbException("Could not handshake with CDDB server")
      case _ => throw CddbException(s"Unknown CDDB error code: ${code}")
    }
  }

  private def readResponseFromBodyLines(bodyLines: Array[String]): CddbReadResponse = {
    val firstLineComponents = bodyLines.head.split(" ")
    val category = firstLineComponents(1)
    val discId = firstLineComponents(2)

    var discTitle = ""
    var discArtist = ""
    val titles = new mutable.Queue[String]()

    bodyLines.drop(1).foreach {
      case DTITLE_REGEX(artist, title) => {
        discTitle = title
        discArtist = artist
      }
      case TITLE_REGEX(title) => titles += title
    }

    CddbReadResponse(
      discId = discId,
      category = category,
      discTitle = discTitle,
      discArtist = discArtist,
      titles = titles.toSeq
    )
  }

  private def responseWithCmd(cmd: String): Future[HttpResponse[String]] = {
    val request = Http(cddbHttpPath)
      .param("cmd", cmd)
      .param("hello", cddbHello)
      .param("proto", "1")
    println(s"Request = $request")

    Future { request.asString }
  }

  private def cddbHello: String = {
    val helloElements = Seq(sys.props.get("user.name").getOrElse("unknown"), "localhost", "giles", "1.0")
    helloElements.mkString("+")
  }
}

object CddbClientImpl {
  private val DTITLE_REGEX = """DTITLE=(.*) / (.*)""".r
  private val TITLE_REGEX = """TITLE\d+=(.*)""".r
}
