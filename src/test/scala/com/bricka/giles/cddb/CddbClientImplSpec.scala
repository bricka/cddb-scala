package com.bricka.giles.cddb

import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers, OptionValues}
import org.scalamock.scalatest.MockFactory

import com.github.kristofa.test.http.{Method, MockHttpServer, SimpleHttpResponseProvider}

class CddbClientImplSpec extends FunSpec with Matchers with MockFactory with BeforeAndAfterEach with OptionValues {
  private val PORT = 10000
  private var server: MockHttpServer = _
  private var responseProvider: SimpleHttpResponseProvider = _

  override def beforeEach() {
    responseProvider = new SimpleHttpResponseProvider
    server = new MockHttpServer(PORT, responseProvider)
    server.start
  }

  override def afterEach() {
    server.stop
  }

  describe("getInfo") {
    it ("handles an exact match") {
      val discId = "discId"
      val off1 = 1L
      val off2 = 2L
      val numSeconds = 10
      val artist = "artist"
      val title = "title"
      val trackTitle1 = "trackTitle1"
      val trackTitle2 = "trackTitle2"
      val trackTitles = Seq(trackTitle1, trackTitle2)
      val category = "category"
      val expectedQueryCommand = s"cddb+query+${discId}+2+${off1}+${off2}+${numSeconds}"
      val expectedReadCommand = s"cddb+read+${category}+${discId}"
      // val expectedHello = s"${sys.props.get("user.name").get}+localhost+giles+1.0"
      val expectedHello = "alex+localhost+giles+1.0"

      val queryResponse = "200 misc 2f0d8939 Dan Levenson / Old Time Festival Tunes For Clawhammer Banjo (Disc 2)"

      responseProvider.expect(Method.GET, s"?cmd=${expectedQueryCommand}&hello=${expectedHello}&proto=1").respondWith(200, "text/plain", queryResponse)
      responseProvider.expect(Method.GET, s"?cmd=${expectedReadCommand}&hello=${expectedHello}&proto=1").respondWith(200, "text/plain", ReadResponse)

      val cddbClient = new CddbClientImpl(s"http://localhost:${PORT}")
      val response = cddbClient.getInfo(discId, 2, Seq(off1, off2), numSeconds)

      server.verify

      response.value should have (
        'discId (discId),
        'artist (artist),
        'title (title),
        'trackTitles (trackTitles)
      )
    }
  }

private val ReadResponse =
  """210 misc 2f0d8939 CD database entry follows (until terminating `.')
# xmcd CD database file
#
# Track frame offsets:
#	150
#	4427
#	8816
#	13225
#	17651
#	21927
#	27177
#	33187
#	37464
#	42715
#	47509
#	53055
#	59654
#	64001
#	68357
#	71106
#	75455
#	78982
#	83477
#	87829
#	92185
#	96525
#	100954
#	105456
#	109919
#	114267
#	118541
#	122902
#	127322
#	131819
#	137888
#	142537
#	147023
#	151352
#	156115
#	161369
#	165721
#	170057
#	174462
#	178852
#	183396
#	187875
#	192228
#	196660
#	201015
#	205378
#	209872
#	213437
#	219549
#	224043
#	228468
#	232927
#	237281
#	241614
#	246987
#	251480
#	255754
#
# Disc length: 3467 seconds
#
# Revision: 0
# Processed by: cddbd v1.5.2PL0 Copyright (c) Steve Scherf et al.
# Submitted via: CDex 1.51
#
DISCID=2f0d8939
DTITLE=Dan Levenson / Old Time Festival Tunes For Clawhammer Banjo (Disc 2)
TTITLE0=June Apple
TTITLE1=Kansas City Reel
TTITLE2=Kitchen Girl
TTITLE3=Leather Britches
TTITLE4=Liberty
TTITLE5=Little Billie Wilson
TTITLE6=Little Rabbit
TTITLE7=Liza Poor Gal
TTITLE8=Logan County Blues
TTITLE9=Lost Indian - 1
TTITLE10=Lost Indian - 2
TTITLE11=Lost Indian - 3
TTITLE12=Magpie
TTITLE13=Martha Campbell
TTITLE14=Mole in the Ground
TTITLE15=Monkey on a Dogcart
TTITLE16=Muddy Roads
TTITLE17=New Five Cents
TTITLE18=Nixon's Farewell
TTITLE19=North Carolina Breakdown
TTITLE20=Old Bunch of Keys
TTITLE21=Old Joe Clark - 1 - major
TTITLE22=Old Joe Clark - 2 - minor
TTITLE23=Old Mother Flanagan
TTITLE24=Pike's Peak
TTITLE25=Possum in a Well
TTITLE26=Possum on a Rail
TTITLE27=Quince Dillon's High D
TTITLE28=Rachael
TTITLE29=Ragtime Annie
TTITLE30=Railroading Across the Rocky Mountains
TTITLE31=Red Haired Boy
TTITLE32=Reuben's Train
TTITLE33=Rochester Schottische
TTITLE34=Rocky Pallet
TTITLE35=Roscoe
TTITLE36=Rush and the Pepper
TTITLE37=Saint Anne's Reel
TTITLE38=Sally Anne Johnson
TTITLE39=Sandy Boys - 1
TTITLE40=Sandy Boys - 2
TTITLE41=Sandy River Belle
TTITLE42=Sarah Armstrong's Tune
TTITLE43=Shuffle About
TTITLE44=Smith's Reel
TTITLE45=Staten Island Hornpipe
TTITLE46=Sugar in the Gourd
TTITLE47=Texas Gals
TTITLE48=Tom and Jerry
TTITLE49=Too Young to Marry
TTITLE50=Wake Up Susan
TTITLE51=Walking in my Sleep
TTITLE52=Washington's March
TTITLE53=Ways of the World
TTITLE54=Whiskey Before Breakfast
TTITLE55=Whistling Rufus
TTITLE56=Yellow Rose of Texas
EXTD= YEAR: 2007
EXTT0=
EXTT1=
EXTT2=
EXTT3=
EXTT4=
EXTT5=
EXTT6=
EXTT7=
EXTT8=
EXTT9=
EXTT10=
EXTT11=
EXTT12=
EXTT13=
EXTT14=
EXTT15=
EXTT16=
EXTT17=
EXTT18=
EXTT19=
EXTT20=
EXTT21=
EXTT22=
EXTT23=
EXTT24=
EXTT25=
EXTT26=
EXTT27=
EXTT28=
EXTT29=
EXTT30=
EXTT31=
EXTT32=
EXTT33=
EXTT34=
EXTT35=
EXTT36=
EXTT37=
EXTT38=
EXTT39=
EXTT40=
EXTT41=
EXTT42=
EXTT43=
EXTT44=
EXTT45=
EXTT46=
EXTT47=
EXTT48=
EXTT49=
EXTT50=
EXTT51=
EXTT52=
EXTT53=
EXTT54=
EXTT55=
EXTT56=
PLAYORDER=
."""
}
