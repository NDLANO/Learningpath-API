/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search


import no.ndla.learningpathapi.LearningpathApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.learningpathapi.integration.JestClientFactory
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{LearningpathApiProperties, TestEnvironment, UnitSuite}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.{Node, NodeBuilder}
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._

import scala.reflect.io.Path
import scala.util.Random


class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esHttpPort = new Random(System.currentTimeMillis()).nextInt(30000 - 20000) + 20000
  val esDataDir = "esTestData"
  var esNode: Node = _

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esHttpPort")
  override val searchConverterService: SearchConverterService = new SearchConverterService
  override val searchIndexService: SearchIndexService = new SearchIndexService
  override val searchService: SearchService = new SearchService

  val paul = Author("author", "Truly Weird Rand Paul")
  val license = "publicdomain"
  val copyright = Copyright(license, List(paul))
  val DefaultLearningPath = LearningPath(
    id = None,
    revision = None, externalId = None, isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = clock.now(),
    tags = List(),
    owner = "owner",
    copyright = copyright)

  val PenguinId = 1
  val BatmanId = 2
  val DonaldId = 3

  override def beforeAll() = {
    Path(esDataDir).deleteRecursively()
    doReturn(api.Author("Forfatter", "En eier")).when(converterService).asAuthor(any[NdlaUserName])

    val today = new DateTime().toDate
    val yesterday = new DateTime().minusDays(1).toDate
    val tomorrow = new DateTime().plusDays(1).toDate

    val thePenguin = DefaultLearningPath.copy(
      id = Some(PenguinId),
      title = List(Title("Pingvinen er en kjeltring", Some("nb"))),
      description = List(Description("Dette handler om fugler", Some("nb"))),
      duration = Some(1),
      lastUpdated = yesterday,
      tags = List(LearningPathTags(Seq("superhelt", "kanikkefly"), Some("nb")))
    )

    val batman = DefaultLearningPath.copy(
      id = Some(BatmanId),
      title = List(Title("Batman er en tøff og morsom helt", Some("nb")), Title("Batman is a tough guy", Some("en"))),
      description = List(Description("Dette handler om flaggermus, som kan ligne litt på en fugl", Some("nb"))),
      duration = Some(2),
      lastUpdated = today,
      tags = List(LearningPathTags(Seq("superhelt", "kanfly"), Some("nb")))
    )

    val theDuck = DefaultLearningPath.copy(
      id = Some(DonaldId),
      title = List(Title("Donald er en tøff, rar og morsom and", Some("nb")), Title("Donald is a weird duck", Some("nb"))),
      description = List(Description("Dette handler om en and, som også minner om både flaggermus og fugler.", Some("nb"))),
      duration = Some(3),
      lastUpdated = tomorrow,
      tags = List(LearningPathTags(Seq("disney", "kanfly"), Some("nb")))
    )


    val settings = Settings.settingsBuilder()
      .put("path.home", esDataDir)
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("http.port", esHttpPort)
      .put("cluster.name", getClass.getName)
      .build()

    esNode = new NodeBuilder().settings(settings).node()
    esNode.start()

    val indexName = searchIndexService.createNewIndex()
    searchIndexService.updateAliasTarget(None, indexName)
    searchIndexService.indexLearningPath(thePenguin)
    searchIndexService.indexLearningPath(batman)
    searchIndexService.indexLearningPath(theDuck)

    blockUntil(() => searchService.countDocuments() == 3)
  }

  override def afterAll() = {
    esNode.close()
    Path(esDataDir).deleteRecursively()
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 100
    val pageSize = 10
    val expectedStartAt = (page - 1) * pageSize
    searchService.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all learningpaths are returned ordered by title descending") {
    val searchResult = searchService.all(None, Sort.ByTitleDesc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by title ascending") {
    val searchResult = searchService.all(None, Sort.ByTitleAsc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(BatmanId)
  }

  test("That order by durationDesc orders search result by duration descending") {
    val searchResult = searchService.all(None, Sort.ByDurationDesc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(DonaldId)
  }

  test("That order ByDurationAsc orders search result by duration ascending") {
    val searchResult = searchService.all(None, Sort.ByDurationAsc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(PenguinId)
  }

  test("That order ByLastUpdatedDesc orders search result by last updated date descending") {
    val searchResult = searchService.all(None, Sort.ByLastUpdatedDesc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(DonaldId)
    searchResult.results.last.id should be(PenguinId)
  }

  test("That order ByLastUpdatedAsc orders search result by last updated date ascending") {
    val searchResult = searchService.all(None, Sort.ByLastUpdatedAsc, Some("nb"), None, None)
    searchResult.totalCount should be(3)
    searchResult.results.head.id should be(PenguinId)
    searchResult.results.last.id should be(DonaldId)
  }

  test("That searching only returns documents matching the query") {
    val searchResult = searchService.matchingQuery(Seq("heltene"), None, Some("nb"), Sort.ByTitleAsc, None, None)
    searchResult.totalCount should be(1)
    searchResult.results.head.id should be(BatmanId)
  }

  test("That searching only returns documents matching the query in the specified language") {
    val searchResult = searchService.matchingQuery(Seq("guy"), None, Some("en"), Sort.ByTitleAsc, None, None)
    searchResult.totalCount should be(1)
    searchResult.results.head.id should be(BatmanId)
  }

  test("That filtering on tag only returns documents where the tag is present") {
    val searchResult = searchService.all(Some("superhelt"), Sort.ByTitleAsc, Some("nb"), None, None)
    searchResult.totalCount should be(2)
    searchResult.results.head.id should be(BatmanId)
    searchResult.results.last.id should be(PenguinId)
  }

  test("That filtering on tag combined with search only returns documents where the tag is present and the search matches the query") {
    val searchResult = searchService.matchingQuery(Seq("heltene"), Some("kanfly"), Some("nb"), Sort.ByTitleAsc, None, None)
    searchResult.totalCount should be(1)
    searchResult.results.head.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman when searching for tough weirdos") {
    val searchResult = searchService.matchingQuery(Seq("tøff", "rar"), None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    searchResult.totalCount should be(2)
    searchResult.results.head.id should be(DonaldId)
    searchResult.results.last.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman and the penguin when searching for duck, bat and bird") {
    val searchResult = searchService.matchingQuery(Seq("and", "flaggermus", "fugl"), None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    searchResult.totalCount should be(3)
    searchResult.results.toList(0).id should be(DonaldId)
    searchResult.results.toList(1).id should be(BatmanId)
    searchResult.results.toList(2).id should be(PenguinId)
  }

  test("That searching and ordering by relevance is not returning Penguin when searching for duck, bat and bird, but filtering on kanfly") {
    val searchResult = searchService.matchingQuery(Seq("and", "flaggermus", "fugl"), Some("kanfly"), Some("nb"), Sort.ByRelevanceDesc, None, None)
    searchResult.totalCount should be(2)
    searchResult.results.head.id should be(DonaldId)
    searchResult.results.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns both Donald and Batman even if it is misspelled") {
    val searchResult = searchService.matchingQuery(Seq("and", "flaggremsu"), None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    searchResult.totalCount should be(2)
    searchResult.results.head.id should be(DonaldId)
    searchResult.results.last.id should be(BatmanId)
  }

  def blockUntil(predicate: () => Boolean) = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }
}
