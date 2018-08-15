package pl.liosedhel.mytrip.worldmap.impl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.AvailableMaps
import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents.WorldMapCreated

class WorldMapsRepository(cassandraSession: CassandraSession) {

  private lazy val selectWorldMapsPreparedStatement: Future[PreparedStatement] = {
    cassandraSession.prepare("SELECT id, creatorId FROM worldmaps")
  }

  def availableMaps(): Future[AvailableMaps] = {
    for {
      statement <- selectWorldMapsPreparedStatement
      result    <- cassandraSession.selectAll(statement.bind())
    } yield AvailableMaps(result.map(_.getString("id")))
  }

  def createWorldMapsTable(): Future[Done] =
    cassandraSession.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS worldmaps ( id TEXT, creatorId TEXT, PRIMARY KEY (id))"
    )

  private lazy val insertWordlMapPreparedStatement: Future[PreparedStatement] = {
    cassandraSession.prepare("INSERT INTO worldmaps (id, creatorId) VALUES (?, ?)")
  }

  def saveMap(
    worldMapCreated: WorldMapCreated
  ): Future[List[BoundStatement]] = {
    insertWordlMapPreparedStatement.map { insertStatement =>
      val insertWorldMap = insertStatement.bind()
      insertWorldMap.setString("id", worldMapCreated.id)
      insertWorldMap.setString("creatorId", worldMapCreated.creatorId)
      List(insertWorldMap)
    }
  }

}
