package pl.liosedhel.mytrip.worldmap.impl

import akka.persistence.query.Offset
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{WorldMap, WorldMapDetails}
import pl.liosedhel.mytrip.worldmap.api._

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorldMapServiceImpl(
  persistentEntityRegistry: PersistentEntityRegistry,
  cassandraReadSide: CassandraReadSide,
  pubSub: PubSubRegistry,
  readSide: ReadSide,
  worldMapsRepository: WorldMapsRepository,
  placesRepository: PlacesRepository
) extends WorldMapService {

  //register event processor
  readSide.register[WorldMapAggregate.WorldMapEvent](
    new WorldMapEventProcessor(cassandraReadSide, worldMapsRepository)
  )

  readSide.register[PlaceAggregate.PlaceEvent](
    new PlaceEventProcessor(placesRepository)
  )

  override def worldMap(mapId: String): ServiceCall[NotUsed, WorldMap] =
    ServiceCall { _ =>
      val worldMapId = WorldMapId(mapId)
      for {
        worldMap <- worldMapsRepository.getMap(worldMapId)
        places   <- placesRepository.getPlaces(worldMapId)
      } yield WorldMapApiModel.WorldMap(worldMapId, worldMap.creatorId, worldMap.description, places)
    }

  override def createWorldMap(): ServiceCall[WorldMapDetails, Done] =
    ServiceCall[WorldMapDetails, Done] { worldMap =>
      val worldMapAggregate =
        persistentEntityRegistry.refFor[WorldMapAggregate](worldMap.mapId.id)
      worldMapAggregate.ask(
        WorldMapAggregate.CreateNewMap(
          worldMap.mapId,
          worldMap.creatorId,
          worldMap.description.getOrElse("")
        )
      )
    }

  override def worldMapCreatedTopic(): Topic[WorldMapApiEvents.WorldMapCreated] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(WorldMapAggregate.WorldMapEvent.Tag, fromOffset)
        .mapConcat(filterWorldMapCreated)
    }
  }

  private def filterWorldMapCreated(
    ev: EventStreamElement[WorldMapAggregate.WorldMapEvent]
  ): immutable.Seq[(WorldMapApiEvents.WorldMapCreated, Offset)] = ev match {
    case EventStreamElement(_, w: WorldMapAggregate.WorldMapCreated, offset) =>
      immutable.Seq((WorldMapApiEvents.WorldMapCreated(w.mapId, w.creatorId, w.description), offset))
    case _ => Nil
  }

  override def placeCreatedTopic(): Topic[WorldMapApiEvents.PlaceAdded] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(PlaceAggregate.PlaceEvent.Tag, fromOffset)
        .mapConcat(filterPlaceCreated)
    }
  }

  private def filterPlaceCreated(
    ev: EventStreamElement[PlaceAggregate.PlaceEvent]
  ): immutable.Seq[(WorldMapApiEvents.PlaceAdded, Offset)] = ev match {
    case EventStreamElement(_, p: PlaceAggregate.PlaceCreated, offset) =>
      immutable.Seq(
        (WorldMapApiEvents.PlaceAdded(p.placeId, p.mapId, p.description, p.coordinates, p.photoLinks), offset)
      )
    case _ => Nil
  }

  override def availableMaps(): ServiceCall[NotUsed, WorldMapApiModel.AvailableMaps] =
    ServiceCall { _ =>
      worldMapsRepository.availableMaps()
    }

  override def createPlace(mapId: String): ServiceCall[WorldMapApiModel.Place, Done] =
    ServiceCall { place =>
      val worldMapId        = WorldMapId(mapId)
      val worldMapAggregate = persistentEntityRegistry.refFor[PlaceAggregate](place.placeId.id)
      worldMapAggregate.ask(
        PlaceAggregate
          .CreateNewPlace(place.placeId, worldMapId, place.description, place.coordinates, place.photoLinks)
      )
    }

  override def placeAddedStream(mapId: String): ServiceCall[NotUsed, Source[WorldMapApiModel.Place, NotUsed]] =
    ServiceCall { _ =>
      val worldMapId = WorldMapId(mapId)
      val topic      = pubSub.refFor(TopicId[PlaceAggregate.Place](worldMapId.id))
      Future.successful(
        topic.subscriber.map(
          p =>
            WorldMapApiModel.Place(
              p.id,
              p.description,
              p.coordinates,
              p.photoLinks
          )
        )
      )
    }

  override def addLink(placeId: String): ServiceCall[WorldMapApiModel.Url, Done] =
    ServiceCall[WorldMapApiModel.Url, Done] { url =>
      val pId            = PlaceId(placeId)
      val placeAggregate = persistentEntityRegistry.refFor[PlaceAggregate](pId.id)
      placeAggregate.ask(PlaceAggregate.AddLink(pId, url))
    }
}
