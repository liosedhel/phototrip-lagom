package pl.liosedhel.mytrip.comments.impl
import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import pl.liosedhel.mytrip.comments.api.{CommentsService, PlaceComment}
import pl.liosedhel.mytrip.worldmap.api.PlaceId

class CommentsServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends CommentsService {
  
  override def addComment(placeId: String): ServiceCall[
    PlaceComment,
    Done
  ] = ServiceCall[PlaceComment, Done] { placeComment =>
    val commentAggregate = persistentEntityRegistry.refFor[PlaceCommentAggregate](placeId)
    commentAggregate.ask(
      PlaceCommentAggregate
        .CreateComment(placeComment.commentId, PlaceId(placeId), placeComment.creatorId, placeComment.comment)
    )
  }
}
