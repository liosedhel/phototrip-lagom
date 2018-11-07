package pl.liosedhel.mytrip.comments.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import pl.liosedhel.mytrip.comments.api.CommentsService
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

class CommentLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new CommentApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new CommentApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[CommentsService])
}

abstract class CommentApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with PubSubComponents
    with AhcWSComponents
    with CORSComponents {

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[CommentsService]({
    val commentRepository = wire[CommentRepository]
    wire[CommentsServiceImpl]
  })

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = PlaceCommentSerializerRegistry

  persistentEntityRegistry.register(wire[PlaceCommentAggregate])

}

