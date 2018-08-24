package pl.liosedhel.mytrip.worldmap.impl

import scala.collection.immutable
import scala.concurrent.Future

import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{NewWorldMap, Place, WorldMap}
import pl.liosedhel.mytrip.worldmap.api.{WorldMapApiEvents, WorldMapApiModel, WorldMapService}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapCommands.{AddLink, AddPlace, CreateNewMap, GetWorldMap}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents.{PlaceAdded, WorldMapCreated, WorldMapEvent}

class WorldMapServiceImpl(
  persistentEntityRegistry: PersistentEntityRegistry,
  cassandraReadSide: CassandraReadSide,
  pubSub: PubSubRegistry,
  readSide: ReadSide,
  cassandraSession: CassandraSession,
  worldMapsRepository: WorldMapsRepository
) extends WorldMapService {

  //register event processor
  readSide.register[WorldMapEvents.WorldMapEvent](
    new WorldMapEventProcessor(cassandraReadSide, worldMapsRepository)
  )

  override def worldMap(id: String): ServiceCall[NotUsed, WorldMap] =
    ServiceCall { _ =>
      val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](id)
      worldMapAggregate.ask(GetWorldMap(id))
    }

  override def createWorldMap(id: String): ServiceCall[NewWorldMap, Done] =
    ServiceCall[NewWorldMap, Done] { worldMap =>
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
      immutable.Seq((WorldMapApiEvents.PlaceAdded(p.placeId, p.worldMapId, p.coordinates, p.photoLinks), offset))
    case _ => Nil
  }

  override def availableMaps(): ServiceCall[NotUsed, WorldMapApiModel.AvailableMaps] =
    ServiceCall { _ =>
      worldMapsRepository.availableMaps()
    }

  override def addPlace(mapId: String): ServiceCall[WorldMapApiModel.Place, Done] =
    ServiceCall { place =>
      val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](mapId)
      worldMapAggregate.ask(AddPlace(place.id, place.coordinates, place.photoLinks))
    }

  override def placeAdded(mapId: String): ServiceCall[NotUsed, Source[WorldMapApiModel.Place, NotUsed]] =
    ServiceCall { _ =>
      val topic = pubSub.refFor(TopicId[Place](mapId))
      val subscriber: Source[Place, NotUsed] = topic.subscriber
      Future.successful(subscriber)
    }

  override def addLink(mapId: String, placeId: String): ServiceCall[WorldMapApiModel.Url, Done] =
    ServiceCall[WorldMapApiModel.Url, Done] { url =>
      val worldMapAggregate = persistentEntityRegistry.refFor[WorldMapAggregate](mapId)
      worldMapAggregate.ask(AddLink(placeId, url))
    }
}
