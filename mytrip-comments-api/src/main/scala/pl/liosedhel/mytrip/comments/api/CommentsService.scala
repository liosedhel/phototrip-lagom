package pl.liosedhel.mytrip.comments.api
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.libs.json._

trait CommentsService extends Service {

  import CommentFormatters._

  def addComment(placeId: String): ServiceCall[PlaceComment, Done]

  def getComments(placeId: String): ServiceCall[NotUsed, List[PlaceComment]]

  def getCommentsStream(placeId: String): ServiceCall[NotUsed, Source[PlaceComment, NotUsed]]

  override def descriptor: Descriptor = {
    import Service._
    named("comments")
      .withCalls(
        pathCall("/api/world-map/place/:id/comment", addComment _),
        pathCall("/api/world-map/place/:id/comments", getComments _),
        pathCall("/api/world-map/place/:id/stream/comments", getCommentsStream _)
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api.*")
      )
  }
}

case class CommentId(id: String)
object CommentId {
  implicit val worldMapIdFormat = new Format[CommentId] {
    override def writes(
      o: CommentId
    ): JsValue =
      Json.toJson(o.id)
    override def reads(
      json: JsValue
    ): JsResult[CommentId] =
      json.validate(implicitly[Reads[String]]).map(apply)
  }
}
case class PlaceComment(commentId: CommentId, creatorId: String, comment: String, timestamp: Long)

object CommentFormatters {
  implicit val placeCommentFormat: Format[PlaceComment] = Json.format[PlaceComment]
}
