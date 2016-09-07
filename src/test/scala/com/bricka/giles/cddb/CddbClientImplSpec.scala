package com.bricka.giles.cddb

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers, OptionValues}
import org.scalatest.concurrent.ScalaFutures

import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.{HttpResponse, Parameter}

import com.bricka.giles.cddb.response.{CddbReadResponse, ExactCddbQueryResponse, InexactCddbQueryResponse}

class CddbClientImplSpec extends FunSpec with Matchers with BeforeAndAfterEach with ScalaFutures with OptionValues {
  var mockServer: ClientAndServer = _

  override def beforeEach: Unit = {
    mockServer = ClientAndServer.startClientAndServer(CddbClientImplSpec.Port)
  }

  override def afterEach: Unit = {
    mockServer.stop
  }

  // scalastyle:off magic.number

  describe("CddbClientImpl") {
    describe("query") {
      def queryCommand(discId: String, trackOffsets: Seq[Long], numSeconds: Long): String =
        s"cddb+query+${discId}+${trackOffsets.size}+${trackOffsets.mkString("+")}+${numSeconds}"

      it("throws exception if request fails") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withStatusCode(500)
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("returns nothing if no record exists") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody(s"202 no match for disc ID ${discId}")
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f) { response =>
          response shouldBe None
        }
      }

      it("throws exception if it receives code 403") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("403")
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("throws exception if it receives code 409") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("409")
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("returns correct response if it receives exact match") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100
        val category = "category"
        val discTitle = "discTitle"
        val discArtist = "discArtist"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody(s"200 ${category} ${discId} ${discArtist} / ${discTitle}")
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f) { response =>
          response.value shouldBe ExactCddbQueryResponse(category = category, discId = discId, discTitle = discTitle, discArtist = discArtist)
        }
      }

      it("returns correct response if it receives inexact matches") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val discId = "discId"
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100
        val discId1 = "discId1"
        val category1 = "category1"
        val discTitle1 = "discTitle1"
        val discArtist1 = "discArtist1"
        val discId2 = "discId2"
        val category2 = "category2"
        val discTitle2 = "discTitle2"
        val discArtist2 = "discArtist2"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", queryCommand(discId, trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody(s"""211 close matches found
${category1} ${discId1} ${discArtist1} / ${discTitle1}
${category2} ${discId2} ${discArtist2} / ${discTitle2}
.""")
          )

        val f = client.query(discId, trackOffsets, numSeconds)

        whenReady(f) { response =>
          response.value shouldBe InexactCddbQueryResponse(responses = Seq(
            ExactCddbQueryResponse(
              category = category1,
              discId = discId1,
              discArtist = discArtist1,
              discTitle = discTitle1
            ),
            ExactCddbQueryResponse(
              category = category2,
              discId = discId2,
              discArtist = discArtist2,
              discTitle = discTitle2
            )
          ))
        }
      }
    }

    describe("read") {
      def readCommand(category: String, discId: String): String =
        s"cddb+read+${category}+${discId}"

      it("throws exception if request fails") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withStatusCode(500)
          )

        val f = client.read(category, discId)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("returns nothing if it receives code 401") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("401")
          )

        val f = client.read(category, discId)

        whenReady(f) { response =>
          response shouldBe None
        }
      }

      it("throws an exception if it receives code 402") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("402")
          )

        val f = client.read(category, discId)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("throws an exception if it receives code 403") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("403")
          )

        val f = client.read(category, discId)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("throws an exception if it receives code 409") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("409")
          )

        val f = client.read(category, discId)

        whenReady(f.failed) { e =>
          e shouldBe a[CddbException]
        }
      }

      it("returns the correct response") {
        val client = new CddbClientImpl(s"http://localhost:${CddbClientImplSpec.Port}")

        val category = "category"
        val discId = "discId"
        val discTitle = "discTitle"
        val discArtist = "discArtist"
        val title0 = "title0"
        val title1 = "title1"
        val title2 = "title2"

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", readCommand(category, discId))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody(s"""210 ${category} ${discId}
# xmcd 2.0 CD database file
# Another comment
DISCID=${discId}
DTITLE=${discArtist} / ${discTitle}
TITLE0=${title0}
TITLE2=${title2}
TITLE1=${title1}
.""")
          )

        val f = client.read(category, discId)

        whenReady(f) { response =>
          response.value shouldBe CddbReadResponse(
            discId = discId,
            category = category,
            discTitle = discTitle,
            discArtist = discArtist,
            titles = Map(
              0 -> title0,
              1 -> title1,
              2 -> title2
            )
          )
        }
      }
    }

    describe("discid") {
      it("returns the correct discid") {
        val trackOffsets = Seq(1L, 2L)
        val numSeconds = 100

        mockServer
          .when(
            request
              .withMethod("GET")
              .withQueryStringParameters(
                new Parameter("cmd", discIdCommand(trackOffsets, numSeconds))
              ),
            Times.exactly(1)
          )
          .respond(
            HttpResponse.response
              .withBody("
          )
      }
    }
  }
}

object CddbClientImplSpec {
  val Port = 9999
}
