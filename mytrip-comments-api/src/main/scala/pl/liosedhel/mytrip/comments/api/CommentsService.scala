package pl.liosedhel.mytrip.comments.api
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.libs.json._

trait CommentsService extends Service {

  import CommentFormatters._

  def addComment(placeId: String): ServiceCall[PlaceComment, Done]

  def getComments(placeId: String): ServiceCall[NotUsed, List[PlaceComment]]

  override def descriptor: Descriptor = {
    import Service._
    named("comments")
      .withCalls(
        pathCall("/api/world-map/place/:id/comment", addComment _),
        pathCall("/api/world-map/place/:id/comments", getComments _)
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
case class PlaceComment(commentId: CommentId, comment: String, creatorId: String, timestamp: Long)

object CommentFormatters {
  implicit val placeCommentFormat: Format[PlaceComment] = Json.format[PlaceComment]
}
