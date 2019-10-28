/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi

import java.net.Socket

import com.zaxxer.hikari.HikariDataSource
import no.ndla.learningpathapi.integration.DataSource
import org.testcontainers.elasticsearch.ElasticsearchContainer
import scalikejdbc._

import scala.util.{Success, Try}

trait IntegrationTestEnvironment extends TestEnvironment {

  val elasticSearchContainer = Try {
    val esVersion = "6.3.2"
    val c = new ElasticsearchContainer(s"docker.elastic.co/elasticsearch/elasticsearch:$esVersion")
    c.start()
    c
  }
  val elasticSearchHost = elasticSearchContainer.map(c => s"http://${c.getHttpHostAddress}")

  def databaseIsAvailable(implicit session: DBSession = ReadOnlyAutoSession): Boolean = {
    val version = Try(
      sql"select version();"
        .map(rs => rs.string("version"))
        .single()
        .apply()
    )

    version.toOption.flatten
      .getOrElse("")
      .nonEmpty
  }

  override val dataSource: HikariDataSource =
    if (serverIsListening)
      DataSource.getHikariDataSource
    else
      mock[HikariDataSource]

  def serverIsListening: Boolean = {
    Try(new Socket(LearningpathApiProperties.MetaServer, LearningpathApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  def connectToDatabase(): Unit = {
    Try(ConnectionPool.singleton(new DataSourceConnectionPool(dataSource)))
    Try(DBMigrator.migrate(dataSource))
  }
}
