package pl.liosedhel.mytrip.worldmap.impl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.AvailableMaps

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

}
