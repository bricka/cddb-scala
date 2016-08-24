package com.bricka.giles.cddb

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import scalaj.http.{Http, HttpResponse}

import com.bricka.giles.cddb.response.{CddbQueryResponse, CddbReadResponse, ExactCddbQueryResponse, InexactCddbQueryResponse}

class CddbClientImpl(cddbHttpPath: String)(implicit ec: ExecutionContext) extends CddbClient {
  import CddbClientImpl._

  override def query(discId: String, trackOffsets: Seq[Long], numSeconds: Int): Future[Option[CddbQueryResponse]] =
    responseWithCmd(s"cddb+query+${discId}+${trackOffsets.size}+${trackOffsets.mkString("+")}+${numSeconds}").map { queryResponse =>
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

  private def exactCddbQueryResponseFromLine(components: Seq[String]): ExactCddbQueryResponse = components match {
    case Seq(categ, discId, rest @ _ *) =>
      val restParts = rest.mkString(" ").split(" / ")

      ExactCddbQueryResponse(
        category = categ,
        discId = discId,
        discTitle = restParts(1),
        discArtist = restParts(0)
      )
  }

  private def inexactCddbQueryResponseFromLines(lines: Array[String]): InexactCddbQueryResponse =
    InexactCddbQueryResponse(
      lines.filterNot(_ == ".").map(_.split(" ").toSeq).map(exactCddbQueryResponseFromLine)
    )

  override def read(category: String, discId: String): Future[Option[CddbReadResponse]] =
    responseWithCmd(s"cddb+read+${category}+${discId}").map { readResponse =>
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
    val titlesByIndex = mutable.Map[Int, String]()

    bodyLines.drop(1).foreach {
      case DTITLE_REGEX(artist, title) => {
        discTitle = title
        discArtist = artist
      }
      case TITLE_REGEX(index, title) => {
        titlesByIndex += (index.toInt -> title)
      }
      case _ =>
    }

    CddbReadResponse(
      discId = discId,
      category = category,
      discTitle = discTitle,
      discArtist = discArtist,
      titles = titlesByIndex.toMap
    )
  }

  private def responseWithCmd(cmd: String): Future[HttpResponse[String]] = {
    val request = Http(cddbHttpPath)
      .param("cmd", cmd)
      .param("hello", cddbHello)
      .param("proto", "1")

    Future { request.asString }
  }

  private def cddbHello: String = {
    val helloElements = Seq(sys.props.get("user.name").getOrElse("unknown"), "localhost", "giles", "1.0")
    helloElements.mkString("+")
  }
}

object CddbClientImpl {
  private val DTITLE_REGEX = """DTITLE=(.*) / (.*)""".r
  private val TITLE_REGEX = """TITLE(\d+)=(.*)""".r
}
