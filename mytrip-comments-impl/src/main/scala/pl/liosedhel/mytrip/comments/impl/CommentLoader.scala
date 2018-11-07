package pl.liosedhel.mytrip.comments.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import pl.liosedhel.mytrip.comments.api.CommentsService
import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

class WorldMapLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new WorldMapApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new WorldMapApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[WorldMapService])
}

abstract class WorldMapApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with PubSubComponents
    with AhcWSComponents
    with CORSComponents {

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[CommentsService]({
    //val worldMapsRepository = wire[WorldMapsRepository]
    //val placesRepository = wire[PlacesRepository]
    wire[CommentsServiceImpl]
  })

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = PlaceCommentSerializerRegistry

  // Register world map aggregate
  persistentEntityRegistry.register(wire[PlaceCommentAggregate])

}

