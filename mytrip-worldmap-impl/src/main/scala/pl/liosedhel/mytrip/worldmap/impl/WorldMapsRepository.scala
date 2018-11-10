package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.{Logger, LoggerFactory}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{AvailableMaps, WorldMapDetails}
import pl.liosedhel.mytrip.worldmap.api.{WorldMapApiModel, WorldMapId}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.WorldMapCreated

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorldMapsRepository(cassandraSession: CassandraSession) {

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[WorldMapsRepository])

  private lazy val selectWorldMapsPreparedStatement: Future[PreparedStatement] = {
    cassandraSession.prepare("SELECT id, creatorId, description FROM worldmaps")
  }

  private lazy val selectMapPreparedStatement: Future[PreparedStatement] = {
    cassandraSession.prepare(s"SELECT id, creatorId, description FROM worldmaps WHERE id = ?")
  }

  def availableMaps(): Future[AvailableMaps] = {
    for {
      statement <- selectWorldMapsPreparedStatement
      result    <- cassandraSession.selectAll(statement.bind())
    } yield
      WorldMapApiModel.AvailableMaps(result.map { m =>
        WorldMapApiModel.WorldMapDetails(
          mapId = WorldMapId(m.getString("id")),
          creatorId = m.getString("creatorId"),
          description = Option(m.getString("description"))
        )
      })
  }

  def getMap(worldMapId: WorldMapId): Future[WorldMapDetails] = {
    for {
      selectMapStatement <- selectMapPreparedStatement
      mapOpt             <- cassandraSession.selectOne(selectMapStatement.bind(worldMapId.id))
    } yield
      mapOpt
        .map { m =>
          WorldMapApiModel.WorldMapDetails(
            mapId = WorldMapId(m.getString("id")),
            creatorId = m.getString("creatorId"),
            description = Option(m.getString("description"))
          )
        }
        .getOrElse {
          // yea, I know... but this should not happen ;)
          // if API is getting valid WorldMapId that should indicate existence of connected to this ID map entity
          throw new RuntimeException(s"Map $worldMapId not found")
        }
  }

  def createWorldMapsTable(): Future[Done] =
    cassandraSession.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS worldmaps ( id TEXT, creatorId TEXT, description TEXT, PRIMARY KEY (id))"
    )

  private lazy val insertWordlMapPreparedStatement: Future[PreparedStatement] = {
    cassandraSession.prepare("INSERT INTO worldmaps (id, creatorId, description) VALUES (?, ?, ?)")
  }

  def saveMap(
    worldMapCreated: WorldMapCreated
  ): Future[List[BoundStatement]] = {

    insertWordlMapPreparedStatement.map { insertStatement =>
      val insertWorldMap = insertStatement.bind()
      insertWorldMap.setString("id", worldMapCreated.mapId.id)
      insertWorldMap.setString("creatorId", worldMapCreated.creatorId)
      insertWorldMap.setString("description", worldMapCreated.description)
      List(insertWorldMap)
    }
  }

}
