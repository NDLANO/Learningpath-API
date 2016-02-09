package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

object LearningpathApiProperties extends LazyLogging {

  var LearningpathApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  val ApplicationPort = 80
  lazy val ContactEmail = get("CONTACT_EMAIL")
  lazy val HostAddr = get("HOST_ADDR")
  lazy val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  lazy val MetaUserName = get("DB_USER_NAME")
  lazy val MetaPassword = get("DB_PASSWORD")
  lazy val MetaResource = get("DB_RESOURCE")
  lazy val MetaServer = get("DB_SERVER")
  lazy val MetaPort = getInt("DB_PORT")
  lazy val MetaSchema = get("DB_SCHEMA")
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val SearchHost = "search-engine"
  lazy val SearchPort = get("SEARCH_ENGINE_ENV_TCP_PORT")
  lazy val SearchClusterName = get("SEARCH_ENGINE_ENV_CLUSTER_NAME")
  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  val Published = "PUBLISHED"
  val Private = "PRIVATE"
  val External = "EXTERNAL"
  val CreatedByNDLA = "CREATED_BY_NDLA"
  val VerifiedByNDLA = "VERIFIED_BY_NDLA"

  val DefaultLanguage = "nb"

  val UsernameHeader = "X-Consumer-Username"

  def setProperties(properties: Map[String, Option[String]]) = {
    properties.foreach(prop => LearningpathApiProps.put(prop._1, prop._2))
  }

  def verify() = {
    val missingProperties = LearningpathApiProps.filter(entry => entry._2.isEmpty).toList
    if (missingProperties.nonEmpty) {
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  private def get(envKey: String): String = {
    LearningpathApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  private def getInt(envKey: String): Integer = {
    get(envKey).toInt
  }
}

object PropertiesLoader {
  val EnvironmentFile = "/learningpath-api.env"

  private def readPropertyFile(): Map[String, Option[String]] = {
    val keys = Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    LearningpathApiProperties.setProperties(readPropertyFile())
    LearningpathApiProperties.verify()
  }
}
