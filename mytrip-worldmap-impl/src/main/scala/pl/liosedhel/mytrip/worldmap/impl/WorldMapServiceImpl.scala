package pl.liosedhel.mytrip.worldmap.impl

import scala.collection.immutable

import akka.persistence.query.Offset
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{NewWorldMap, WorldMap}
import pl.liosedhel.mytrip.worldmap.api.{WorldMapApiEvents, WorldMapApiModel, WorldMapService}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapCommands.{AddPlace, CreateNewMap, GetWorldMap}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents.{PlaceAdded, WorldMapCreated, WorldMapEvent}

class WorldMapServiceImpl(
  persistentEntityRegistry: PersistentEntityRegistry,
  cassandraReadSide: CassandraReadSide,
  readSide: ReadSide,
  cassandraSession: CassandraSession,
  worldMapsRepository: WorldMapsRepository
) extends WorldMapService {

  readSide.register[WorldMapEvents.WorldMapEvent](new WorldMapEventProcessor(cassandraReadSide, cassandraSession))

  override def worldMap(id: String): ServiceCall[NotUsed, WorldMap] = ServiceCall { _ =>
    val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](id)
    worldMapAggregate.ask(GetWorldMap(id))
  }

  override def createWorldMap(id: String): ServiceCall[NewWorldMap, Done] = ServiceCall[NewWorldMap, Done] { worldMap =>
    val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](id)
    worldMapAggregate.ask(CreateNewMap(id, worldMap.creatorId))
  }

  override def worldMapCreatedTopic(): Topic[WorldMapApiEvents.WorldMapCreated] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(WorldMapEvents.WorldMapEvent.Tag, fromOffset)
        .mapConcat(filterWorldMapCreated)
    }
  }

  private def filterWorldMapCreated(
    ev: EventStreamElement[WorldMapEvent]
  ): immutable.Seq[(WorldMapApiEvents.WorldMapCreated, Offset)] = ev match {
    case EventStreamElement(_, w: WorldMapCreated, offset) =>
      immutable.Seq((WorldMapApiEvents.WorldMapCreated(w.id, w.creatorId), offset))
    case _ => Nil
  }

  override def placeAddedTopic(): Topic[WorldMapApiEvents.PlaceAdded] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(WorldMapEvents.WorldMapEvent.Tag, fromOffset)
        .mapConcat(filterPlaceAdded)
    }
  }

  private def filterPlaceAdded(
    ev: EventStreamElement[WorldMapEvent]
  ): immutable.Seq[(WorldMapApiEvents.PlaceAdded, Offset)] = ev match {
    case EventStreamElement(_, p: PlaceAdded, offset) =>
      immutable.Seq((WorldMapApiEvents.PlaceAdded(p.worldMapId, p.coordinates, p.photoLinks), offset))
    case _ => Nil
  }

  override def availableMaps(): ServiceCall[NotUsed, WorldMapApiModel.AvailableMaps] = ServiceCall { _ =>
    worldMapsRepository.availableMaps()
  }

  override def addPlace(id: String): ServiceCall[WorldMapApiModel.Place, Done] = ServiceCall { place =>
    val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](id)
    worldMapAggregate.ask(AddPlace(place.coordinates, place.photoLinks))
  }
}
