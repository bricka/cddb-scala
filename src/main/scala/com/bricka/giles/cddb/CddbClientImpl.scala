package com.bricka.giles.cddb

import scala.collection.mutable

import scalaj.http.{Http, HttpResponse}

class CddbClientImpl(cddbHttpPath: String) extends CddbClient {
  import CddbClientImpl._

  override def getInfo(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): Option[CddbInfo] = {
    val queryResponse = responseWithCmd(cddbQueryCommand(discId, numTracks, trackOffsets, numSeconds))

    getCategoryForQueryResponse(queryResponse)
      .map(category => responseWithCmd(cddbReadCommand(category, discId)))
      .flatMap(createCddbInfoFromCddb(_))
  }

  private def responseWithCmd(cmd: String): HttpResponse[String] = {
    val request = Http(cddbHttpPath)
      .param("cmd", cmd)
      .param("hello", cddbHello)
      .param("proto", "1")
    println(s"Request = $request")
    request.asString
  }

  private def getCategoryForQueryResponse(response: HttpResponse[String]): Option[String] = {
    if (response.code != 200) {
      None
    } else if (response.body.startsWith("200 ")) {
      Some(response.body.split(' ')(1))
    } else {
      // TODO: Support multiple matches somehow
      None
    }
  }

  private def createCddbInfoFromCddb(response: HttpResponse[String]): Option[CddbInfo] = {
    if (response.code != 200) {
      None
    } else if (!response.body.startsWith("210 ")) {
      None
    } else {
      createCddbInfoFromSuccessfulCddbResponse(response.body)
    }
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

  private def cddbQueryCommand(discId: String, numTracks: Int, trackOffsets: Seq[Long], numSeconds: Int): String = {
    val commandElements = Seq("cddb", "query", discId, numTracks.toString) ++ trackOffsets.map(_.toString) ++ Seq(numSeconds.toString)
    commandElements.mkString("+")
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
