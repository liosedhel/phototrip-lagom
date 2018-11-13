package pl.liosedhel.mytrip.comments.impl
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRegistry, ReadSide}
import pl.liosedhel.mytrip.comments.api.{CommentsService, PlaceComment}
import pl.liosedhel.mytrip.worldmap.api.PlaceId

import scala.concurrent.Future

class CommentsServiceImpl(
  readSide: ReadSide,
  persistentEntityRegistry: PersistentEntityRegistry,
  commentRepository: CommentRepository
) extends CommentsService {

  readSide.register[PlaceCommentAggregate.PlaceCommentEvent](
    new CommentsProcessor(commentRepository)
  )

  override def addComment(placeId: String): ServiceCall[PlaceComment, Done] =
    ServiceCall[PlaceComment, Done] { placeComment =>
      val commentAggregate = persistentEntityRegistry.refFor[PlaceCommentAggregate](placeComment.commentId.id)
      commentAggregate.ask(
        PlaceCommentAggregate
          .CreateComment(placeComment.commentId, PlaceId(placeId), placeComment.creatorId, placeComment.comment)
      )
    }

  override def getComments(placeId: String): ServiceCall[NotUsed, List[PlaceComment]] = {
    ServiceCall[NotUsed, List[PlaceComment]] { _ => Future.successful(commentRepository.getComment(PlaceId(placeId)))}
  }
}
