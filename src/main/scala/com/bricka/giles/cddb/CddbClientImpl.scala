package com.bricka.giles.cddb

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import scalaj.http.{Http, HttpResponse}

class CddbClientImpl(cddbHttpPath: String)(implicit ec: ExecutionContext) extends CddbClient {
  import CddbClient._
  import CddbClient.response._
  import CddbClientImpl._

  override def query(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): Future[Option[CddbQueryResponse]] = Future {
    val queryResponse = responseWithCmd(s"cddb+query+${discId}+${numTracks}+${trackOffsets.mkString("+")}+${numSeconds}")

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

  private def responseWithCmd(cmd: String): HttpResponse[String] = {
    val request = Http(cddbHttpPath)
      .param("cmd", cmd)
      .param("hello", cddbHello)
      .param("proto", "1")
    println(s"Request = $request")
    request.asString
  }

  private def createCddbInfoFromSuccessfulCddbResponse(body: String): Option[CddbInfo] = {
    var discIdOption: Option[String] = None
    var titleOption: Option[String] = None
    var artistOption: Option[String] = None
    val titles = new mutable.Queue[String]

    for (line <- body.split('\n')) {
      line match {
        case DISCID_REGEX(discId) => discIdOption = Some(discId)
        case DTITLE_REGEX(artist, title) => {
          artistOption = Some(artist)
          titleOption = Some(title)
        }
        case TITLE_REGEX(title) => titles += title
      }
    }

    for {
      discId <- discIdOption
      title <- titleOption
      artist <- artistOption
      if titles.nonEmpty
    } yield {
      new CddbInfo(discId, artist, title, titles.toSeq)
    }
  }

  private def cddbReadCommand(category: String, discId: String): String = {
    val commandElements = Seq("cddb", "read", category, discId)
    commandElements.mkString("+")
  }

  private def cddbHello: String = {
    val helloElements = Seq(sys.props.get("user.name").getOrElse("unknown"), "localhost", "giles", "1.0")
    helloElements.mkString("+")
  }
}

object CddbClientImpl {
  private val DISCID_REGEX = """DISCID=(.*)""".r
  private val DTITLE_REGEX = """DTITLE=(.*) / (.*)""".r
  private val TITLE_REGEX = """TITLE\d+=(.*)""".r
}
