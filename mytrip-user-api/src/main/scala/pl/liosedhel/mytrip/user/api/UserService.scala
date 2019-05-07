package pl.liosedhel.mytrip.user.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.libs.json._
import pl.liosedhel.mytrip.user.api.UserApiEvents.UserCreated
import pl.liosedhel.mytrip.user.api.UserApiFormatters._
import pl.liosedhel.mytrip.user.api.UserApiModel._

object UserService {
  val USER_CREATED = "world-map-created"
  val PLACE_ADDED  = "place-added"
}

trait UserService extends Service {

  def user(id: String): ServiceCall[NotUsed, User]

  def createUser(): ServiceCall[User, Done]

  //topics available externally
  def userCreatedTopic(): Topic[UserCreated]

  override def descriptor: Descriptor = {
    import Service._
    named("worldmapuser")
      .withCalls(
        pathCall("/api/world-map/:id", user _),
        pathCall("/api/user", createUser _)
      )
      .withTopics(
        topic(UserService.USER_CREATED, userCreatedTopic())
        // Kafka partitions messages, messages within the same partition will
        // be delivered in order, to ensure that all messages for the same user
        // go to the same partition (and hence are delivered in order with respect
        // to that user), we configure a partition key strategy that extracts the
        // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[UserCreated](_.id.id)
          )
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api.*")
      )
  }
}

case class UserId(id: String)
object UserId {
  implicit val worldMapIdFormat = new Format[UserId] {
    override def writes(
      o: UserId
    ): JsValue =
      Json.toJson(o.id)
    override def reads(
      json: JsValue
    ): JsResult[UserId] =
      json.validate(implicitly[Reads[String]]).map(apply)
  }
}

object UserApiModel {

  case class User(id: UserId, email: String)
}

object UserApiEvents {
  case class UserCreated(id: UserId, email: String)
}

object UserApiFormatters {
  implicit val userFormat: Format[User]               = Json.format[User]
  implicit val userCreatedFormat: Format[UserCreated] = Json.format[UserCreated]
}
