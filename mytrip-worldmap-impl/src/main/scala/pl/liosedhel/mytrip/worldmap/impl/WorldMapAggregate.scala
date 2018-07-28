package pl.liosedhel.mytrip.worldmap.impl

import scala.collection.immutable.Seq

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters._
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._
import WorldMapCommands._
import WorldMapEvents._
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel

class WorldMapAggregate extends PersistentEntity {

  override type Command = WorldMapCommand[_]
  override type Event   = WorldMapEvent

  //this should be mapped to some internal state, API domain object used directly for the sake of simplicity
  override type State = WorldMapApiModel.WorldMap

  override def initialState: WorldMap = WorldMap("", "", Nil)

  override def behavior: Behavior = {
    case WorldMap("", "", Nil) =>
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
            WorldMap(id, creatorId, Nil)
        }

    case WorldMap(id, creatorId, places) =>
      Actions()
        .onReadOnlyCommand[CreateNewMap, Done] {
          case (_: CreateNewMap, ctx, _) =>
            ctx.invalidCommand(s"Map with ID: $id was already created.")
        }
        .onCommand[AddPlace, Done] {
          case (AddPlace(coordinates, photoLinks), ctx, _) =>
            ctx.thenPersist(
              PlaceAdded(id, coordinates, photoLinks)
            ) { _ =>
              ctx.reply(Done)
            }
        }
        .onReadOnlyCommand[GetWorldMap, WorldMap] {
          case (GetWorldMap(_), ctx, worldMap) =>
            ctx.reply(worldMap)
        }
        .onEvent {
          case (PlaceAdded(_, coordinates, photoLinks), _) =>
            WorldMap(id, creatorId, places :+ Place(coordinates, photoLinks))
        }
  }
}

object WorldMapCommands {
  sealed trait WorldMapCommand[R]                                     extends ReplyType[R]
  case class CreateNewMap(id: String, creatorId: String)              extends WorldMapCommand[Done]
  case class AddPlace(coordinates: Coordinates, photoLinks: Set[Url]) extends WorldMapCommand[Done]
  case class GetWorldMap(id: String)                                  extends WorldMapCommand[WorldMapApiModel.WorldMap]
}

object WorldMapEvents {
  object WorldMapEvent {
    val Tag = AggregateEventTag[WorldMapEvent]
  }
  sealed trait WorldMapEvent extends AggregateEvent[WorldMapEvent] {
    def aggregateTag = WorldMapEvent.Tag
  }

  case class WorldMapCreated(id: String, creatorId: String)                                 extends WorldMapEvent
  case class PlaceAdded(worldMapId: String, coordinates: Coordinates, photoLinks: Set[Url]) extends WorldMapEvent
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
