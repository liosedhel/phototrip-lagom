package pl.liosedhel.mytrip.comments.impl
import pl.liosedhel.mytrip.comments.api.PlaceComment
import pl.liosedhel.mytrip.comments.impl.PlaceCommentAggregate.CommentCreated
import pl.liosedhel.mytrip.worldmap.api.PlaceId

class CommentRepository {

  var places = Map.empty[PlaceId, List[PlaceComment]]

  def save(placeCommentCreated: CommentCreated) = {
    import placeCommentCreated._
    val currentComments = places.getOrElse(placeId, Nil)
    places = places.updated(placeId, currentComments :+ PlaceComment(commentId, creatorId, comment, timestamp))
  }
}
