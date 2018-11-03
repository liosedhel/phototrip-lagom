package pl.liosedhel.mytrip.worldmap.impl

import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate._

class WorldMapEventProcessor(
  readSide: CassandraReadSide,
  worldMapsRepository: WorldMapsRepository
) extends ReadSideProcessor[WorldMapEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[WorldMapEvent] = {
    val builder = readSide.builder[WorldMapEvent]("worldmaps")
    builder
      .setGlobalPrepare(() => worldMapsRepository.createWorldMapsTable())
      .setEventHandler[WorldMapCreated](
        eventStreamElement => worldMapsRepository.saveMap(eventStreamElement.event)
      )
      .build()
  }

  override def aggregateTags = Set(WorldMapEvent.Tag)

}
