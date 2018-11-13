package pl.liosedhel.mytrip.comments.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import pl.liosedhel.mytrip.comments.api.CommentId
import pl.liosedhel.mytrip.worldmap.api.PlaceId
import play.api.libs.json.Json

import scala.collection.immutable.Seq

object PlaceCommentAggregate {

  trait PlaceCommentState
  case class PlaceCommentUninitialized() extends PlaceCommentState
  case class PlaceComment(
    commentId: CommentId,
    placeId: PlaceId,
    creatorId: String,
    comment: String,
    timestamp: Long
  ) extends PlaceCommentState

  trait PlaceCommentCommand[R] extends ReplyType[R]
  case class CreateComment(
    commentId: CommentId,
    placeId: PlaceId,
    creatorId: String,
    comment: String,
    timestamp: Long = System.currentTimeMillis()
  ) extends PlaceCommentCommand[Done]
  case class GetComment(commentId: CommentId) extends PlaceCommentCommand[PlaceComment]

  object PlaceCommentEvent {
    val Tag = AggregateEventTag[PlaceCommentEvent]
  }

  trait PlaceCommentEvent extends AggregateEvent[PlaceCommentEvent] {
    def aggregateTag: AggregateEventTag[PlaceCommentEvent] = PlaceCommentEvent.Tag
  }

  case class CommentCreated(
    commentId: CommentId,
    placeId: PlaceId,
    creatorId: String,
    comment: String,
    timestamp: Long = System.currentTimeMillis()
  ) extends PlaceCommentEvent
}

class PlaceCommentAggregate extends PersistentEntity {
  import PlaceCommentAggregate._

  override type Command = PlaceCommentCommand[_]
  override type Event   = PlaceCommentEvent
  override type State   = PlaceCommentState

  override def initialState: State = PlaceCommentUninitialized()
  override def behavior: Behavior = {
    case PlaceCommentUninitialized() =>
      Actions()
        .onCommand[CreateComment, Done] {
          case (CreateComment(commentId, placeId, creatorId, comment, timestamp), ctx, _) =>
            ctx.thenPersist(
              CommentCreated(commentId, placeId, creatorId, comment, timestamp)
            ) {
              _ => ctx.reply(Done)
            }
        }
        .onReadOnlyCommand[GetComment, PlaceComment] {
          case (GetComment(id), ctx, _) =>
            ctx.invalidCommand(s"Map with ID: $id does not exists yet.")
        }
        .onEvent {
          case (CommentCreated(commentId, placeId, creatorId, comment, timestamp), _) =>
            PlaceComment(commentId, placeId, creatorId, comment, timestamp)
        }

    case _: PlaceComment =>
      Actions()
        .onReadOnlyCommand[CreateComment, Done] {
          case (c: CreateComment, ctx, _) =>
            ctx.invalidCommand(s"Map with ID: ${c.commentId} was already created.")
        }
        .onReadOnlyCommand[GetComment, PlaceComment] {
          case (GetComment(_), ctx, placeComment: PlaceComment) =>
            ctx.reply(placeComment)
        }
  }
}

object PlaceCommentSerializerRegistry extends JsonSerializerRegistry {
  import PlaceCommentAggregate._

  implicit val placeCommentFormat = Json.format[PlaceComment]

  implicit val createPlaceComment = Json.format[CreateComment]
  implicit val getCommentFormat   = Json.format[GetComment]

  implicit val commentCreatedFormat = Json.format[CommentCreated]

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    //model
    JsonSerializer[PlaceComment],
    //commands
    JsonSerializer[CreateComment],
    JsonSerializer[GetComment],
    //events
    JsonSerializer[CommentCreated],
  )
}
