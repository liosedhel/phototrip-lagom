package pl.liosedhel.mytrip.worldmap.impl

import scala.collection.immutable.Seq

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}
import play.api.libs.json.{Format, Json}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters._
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._
import pl.liosedhel.mytrip.worldmap.impl.WorldMapCommands._
import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents._

class WorldMapAggregate(pubSubRegistry: PubSubRegistry) extends PersistentEntity {

  override type Command = WorldMapCommand[_]
  override type Event   = WorldMapEvent

  //this should be mapped to some internal state, API domain object used directly for the sake of simplicity
  override type State = WorldMapApiModel.WorldMap

  override def initialState: WorldMap = WorldMap("", "", Set.empty)

  override def behavior: Behavior = {
    case WorldMap("", "", _) =>
      Actions()
        .onCommand[CreateNewMap, Done] {
          case (CreateNewMap(id, creatorId), ctx, _) =>
            ctx.thenPersist(
              WorldMapCreated(id, creatorId)
            ) { _ =>
              ctx.reply(Done)
            }
        }
        .onReadOnlyCommand[GetWorldMap, WorldMap] {
          case (GetWorldMap(id), ctx, _) =>
            ctx.invalidCommand(s"Map with ID: $id does not exists yet.")
        }
        .onEvent {
          case (WorldMapCreated(id, creatorId), _) =>
            WorldMap(id, creatorId, Set.empty)
        }

    case WorldMap(mapId, creatorId, places) =>
      Actions()
        .onReadOnlyCommand[CreateNewMap, Done] {
          case (_: CreateNewMap, ctx, _) =>
            ctx.invalidCommand(s"Map with ID: $mapId was already created.")
        }
        .onCommand[AddPlace, Done] {
          case (AddPlace(placeId, coordinates, photoLinks), ctx, _) =>
            ctx.thenPersist(
              PlaceAdded(mapId, placeId, coordinates, photoLinks)
            ) { _ =>
              //send added place to internal topic
              val place = Place(placeId, coordinates, photoLinks)
              val topic = pubSubRegistry.refFor(TopicId[Place](mapId))
              topic.publish(place)

              ctx.reply(Done)
            }
        }
        .onCommand[AddLink, Done] {
          case (a: AddLink, ctx, _) if !places.exists(_.id == a.placeId) =>
            ctx.invalidCommand(s"There is no place with id: ${a.placeId}")
            ctx.done
          case (AddLink(placeId, link), ctx, _) =>
            ctx.thenPersist(
              LinkAdded(mapId, placeId, link)
            ) { _ =>
              ctx.reply(Done)
            }
        }
        .onReadOnlyCommand[GetWorldMap, WorldMap] {
          case (GetWorldMap(_), ctx, worldMap) =>
            ctx.reply(worldMap)
        }
        .onEvent {
          case (PlaceAdded(_, placeId, coordinates, photoLinks), _) =>
            WorldMap(mapId, creatorId, places + Place(placeId, coordinates, photoLinks))
        }
        .onEvent {
          case (LinkAdded(_, placeId, url), currentState) =>
            val updatedPlace =
              places.find(_.id == placeId).map(place => place.copy(photoLinks = place.photoLinks + url))
            currentState.copy(places = places.filter(_.id != placeId) ++ updatedPlace.toSet)
        }
  }
}

object WorldMapCommands {
  sealed trait WorldMapCommand[R]                                                 extends ReplyType[R]
  case class CreateNewMap(id: String, creatorId: String)                          extends WorldMapCommand[Done]
  case class AddPlace(id: String, coordinates: Coordinates, photoLinks: Set[Url]) extends WorldMapCommand[Done]
  case class AddLink(placeId: String, url: Url)                                   extends WorldMapCommand[Done]
  case class GetWorldMap(id: String)                                              extends WorldMapCommand[WorldMapApiModel.WorldMap]
}

object WorldMapEvents {
  object WorldMapEvent {
    val Tag = AggregateEventTag[WorldMapEvent]
  }
  sealed trait WorldMapEvent extends AggregateEvent[WorldMapEvent] {
    def aggregateTag = WorldMapEvent.Tag
  }

  case class WorldMapCreated(id: String, creatorId: String) extends WorldMapEvent
  case class PlaceAdded(worldMapId: String, placeId: String, coordinates: Coordinates, photoLinks: Set[Url])
    extends WorldMapEvent
  case class LinkAdded(worldMapId: String, placeId: String, url: Url) extends WorldMapEvent
}

object WorldMapSerializerRegistry extends JsonSerializerRegistry {
  import WorldMapJsonFormatters._
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    //model
    JsonSerializer[WorldMap],
    //commands
    JsonSerializer[CreateNewMap],
    JsonSerializer[AddPlace],
    JsonSerializer[GetWorldMap],
    //events
    JsonSerializer[WorldMapCreated],
    JsonSerializer[PlaceAdded]
  )
}

object WorldMapJsonFormatters {
  //commands
  implicit val createNewMapFormat: Format[CreateNewMap] = Json.format[CreateNewMap]
  implicit val addPlaceFormat: Format[AddPlace]         = Json.format[AddPlace]
  implicit val getWorldMapFormat: Format[GetWorldMap]   = Json.format[GetWorldMap]

  //events
  implicit val worldMapCreated: Format[WorldMapCreated] = Json.format
  implicit val placeAddedFormat: Format[PlaceAdded]     = Json.format[PlaceAdded]
}
