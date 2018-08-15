package pl.liosedhel.mytrip.worldmap.impl

import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents.{WorldMapCreated, WorldMapEvent}

class WorldMapEventProcessor(
  readSide: CassandraReadSide,
  session: CassandraSession,
  worldMapsRepository: WorldMapsRepository
) extends ReadSideProcessor[WorldMapEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[WorldMapEvent] = {
    val builder = readSide.builder[WorldMapEvent]("worldmaps")
    builder
      .setGlobalPrepare(() => worldMapsRepository.createWorldMapsTable())
      .setEventHandler[WorldMapCreated](eventStreamElement => worldMapsRepository.saveMap(eventStreamElement.event))
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[WorldMapEvent]] = Set(WorldMapEvent.Tag)

}
