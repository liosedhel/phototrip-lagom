package pl.liosedhel.mytrip.worldmap.impl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}

import WorldMapEvents.{WorldMapCreated, WorldMapEvent}

class WorldMapEventProcessor(readSide: CassandraReadSide, session: CassandraSession)
  extends ReadSideProcessor[WorldMapEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[WorldMapEvent] = {
    val builder = readSide.builder[WorldMapEvent]("worldmaps")
    builder
      .setGlobalPrepare(() => createWorldMapsTable())
      .setPrepare(_ => prepareWriteTitle())
      .setEventHandler[WorldMapCreated](processWorldMapCreated)
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[WorldMapEvent]] = Set(WorldMapEvent.Tag)

  private def createWorldMapsTable(): Future[Done] =
    session.executeCreateTable("CREATE TABLE IF NOT EXISTS worldmaps ( id TEXT, creatorId TEXT, PRIMARY KEY (id))")

  private val writeMapPromise                     = Promise[PreparedStatement] // initialized in prepare
  private def writeMap: Future[PreparedStatement] = writeMapPromise.future

  private def prepareWriteTitle(): Future[Done] = {
    val f = session.prepare("INSERT INTO worldmaps (id, creatorId) VALUES (?, ?)")
    writeMapPromise.completeWith(f)
    f.map(_ => Done)
  }

  private def processWorldMapCreated(worldMapCreatedElement: EventStreamElement[WorldMapCreated]): Future[List[BoundStatement]] = {
    writeMap.map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("id", worldMapCreatedElement.event.id)
      bindWriteTitle.setString("creatorId", worldMapCreatedElement.event.creatorId)
      List(bindWriteTitle)
    }
  }
}
