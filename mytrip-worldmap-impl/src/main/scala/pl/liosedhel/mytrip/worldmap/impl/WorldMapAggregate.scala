package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import pl.liosedhel.mytrip.worldmap.api.WorldMapId
import play.api.libs.json.Json

import scala.collection.immutable.Seq

object WorldMapAggregate {
  sealed trait WorldMapState
  case object UninitializedWordMap                                            extends WorldMapState
  case class WorldMap(id: WorldMapId, creatorId: String, description: String) extends WorldMapState

  sealed trait WorldMapCommand[R]                                                    extends ReplyType[R]
  case class CreateNewMap(mapId: WorldMapId, creatorId: String, description: String) extends WorldMapCommand[Done]

  case class GetWorldMap(mapId: WorldMapId) extends WorldMapCommand[WorldMap]

  object WorldMapEvent {
    val Tag = AggregateEventTag[WorldMapEvent]
  }
  sealed trait WorldMapEvent extends AggregateEvent[WorldMapEvent] {
    def aggregateTag: AggregateEventTag[WorldMapEvent] = WorldMapEvent.Tag
  }

  case class WorldMapCreated(mapId: WorldMapId, creatorId: String, description: String) extends WorldMapEvent
}

class WorldMapAggregate(pubSubRegistry: PubSubRegistry) extends PersistentEntity {

  import WorldMapAggregate._

  override type Command = WorldMapCommand[_]
  override type Event   = WorldMapEvent

  override type State = WorldMapAggregate.WorldMapState

  override def initialState: WorldMapAggregate.WorldMapState = UninitializedWordMap

  override def behavior: Behavior = {
    case UninitializedWordMap =>
      Actions()
        .onCommand[CreateNewMap, Done] {
          case (CreateNewMap(id, creatorId, description), ctx, _) =>
            ctx.thenPersist(
              WorldMapCreated(id, creatorId, description)
            ) { _ => ctx.reply(Done)
            }
        }
        .onReadOnlyCommand[GetWorldMap, WorldMap] {
          case (GetWorldMap(id), ctx, _) =>
            ctx.invalidCommand(s"Map with ID: $id does not exists yet.")
        }
        .onEvent {
          case (WorldMapCreated(id, creatorId, description), _) =>
            WorldMap(id, creatorId, description)
        }

    case _: WorldMap =>
      Actions()
        .onReadOnlyCommand[CreateNewMap, Done] {
          case (_: CreateNewMap, ctx, WorldMap(mapId, _, _)) =>
            ctx.invalidCommand(s"Map with ID: $mapId was already created.")
        }
        .onReadOnlyCommand[GetWorldMap, WorldMap] {
          case (GetWorldMap(_), ctx, worldMap: WorldMap) =>
            ctx.reply(worldMap)
        }
  }
}

object WorldMapSerializerRegistry extends JsonSerializerRegistry {
  import WorldMapAggregate._

  implicit val worldMapFormat     = Json.format[WorldMap]
  implicit val createNewMapFormat = Json.format[CreateNewMap]
  implicit val getWorldMapFormat  = Json.format[GetWorldMap]

  implicit val worldMapCreatedFormat = Json.format[WorldMapCreated]

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    //model
    JsonSerializer[WorldMap],
    //commands
    JsonSerializer[CreateNewMap],
    JsonSerializer[GetWorldMap],
    //events
    JsonSerializer[WorldMapCreated]
  )
}
