package pl.liosedhel.mytrip.worldmap.impl
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{Coordinates, Url}
import pl.liosedhel.mytrip.worldmap.api.{PlaceId, WorldMapId}
import play.api.libs.json.Json

import scala.collection.immutable.Seq

object PlaceAggregate {
  sealed trait PlaceState
  case object UninitializedPlace extends PlaceState
  case class Place(id: PlaceId, mapId: WorldMapId, description: String, coordinates: Coordinates, photoLinks: Set[Url])
    extends PlaceState {
    def addLink(link: Url): Place =
      copy(photoLinks = photoLinks + link)
  }

  //commands
  sealed trait PlaceCommand[R] extends ReplyType[R]
  case class CreateNewPlace(
    placeId: PlaceId,
    mapId: WorldMapId,
    description: String,
    coordinates: Coordinates,
    photoLinks: Set[Url]
  ) extends PlaceCommand[Done]
  case class AddLink(placeId: PlaceId, url: Url) extends PlaceCommand[Done]
  case class GetPlace(placeId: PlaceId)          extends PlaceCommand[Place]

  //events
  object PlaceEvent {
    val Tag = AggregateEventTag[PlaceEvent]
  }
  sealed trait PlaceEvent extends AggregateEvent[PlaceEvent] {
    def aggregateTag: AggregateEventTag[PlaceEvent] = PlaceEvent.Tag
  }

  case class PlaceCreated(
    placeId: PlaceId,
    mapId: WorldMapId,
    description: String,
    coordinates: Coordinates,
    photoLinks: Set[Url]
  ) extends PlaceEvent
  case class LinkAdded(placeId: PlaceId, url: Url) extends PlaceEvent
}

class PlaceAggregate(pubSubRegistry: PubSubRegistry) extends PersistentEntity {

  import PlaceAggregate._

  override type Command = PlaceCommand[_]
  override type Event   = PlaceEvent

  override type State = PlaceAggregate.PlaceState
  override def initialState: PlaceAggregate.PlaceState = UninitializedPlace

  override def behavior: Behavior = {
    case UninitializedPlace =>
      Actions()
        .onCommand[CreateNewPlace, Done] {
          case (CreateNewPlace(placeId, mapId, description, coordinates, photoLinks), ctx, _) =>
            ctx.thenPersist(
              PlaceCreated(placeId, mapId, description, coordinates, photoLinks)
            ) { _ =>
              ctx.reply(Done)
            }
        }
        .onEvent {
          case (PlaceCreated(placeId, worldMapId, description, coordinates, photoLinks), _) =>
            //send place created to internal topic
            val place = Place(placeId, worldMapId, description, coordinates, photoLinks)
            val topic = pubSubRegistry.refFor(TopicId[Place](worldMapId.id))
            topic.publish(place)
            place

        }
    case _: Place =>
      Actions()
        .onCommand[AddLink, Done] {
          case (AddLink(placeId, url), ctx, _) =>
            ctx.thenPersist(
              LinkAdded(placeId, url)
            ) { _ => ctx.reply(Done)
            }
        }
        .onEvent {
          case (LinkAdded(_, url), place: Place) =>
            place.addLink(url)
        }
        .onReadOnlyCommand[GetPlace, Place] {
          case (GetPlace(placeId), ctx, place: Place) => ctx.reply(place)
        }
  }
}

object PlaceSerializers extends JsonSerializerRegistry {
  import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters.{coordinatesFormat, urlFormat}
  import PlaceAggregate._

  implicit val placeFormat = Json.format[Place]

  implicit val createNewPlaceFormat = Json.format[CreateNewPlace]
  implicit val addLinkFormat        = Json.format[AddLink]
  implicit val getPlaceFormat       = Json.format[GetPlace]

  implicit val placeCreatedFormat = Json.format[PlaceCreated]
  implicit val linkAddedFormat    = Json.format[LinkAdded]

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    //model
    JsonSerializer[Place],
    //commands
    JsonSerializer[CreateNewPlace],
    JsonSerializer[AddLink],
    JsonSerializer[GetPlace],
    //events
    JsonSerializer[PlaceCreated],
    JsonSerializer[LinkAdded]
  )
}
