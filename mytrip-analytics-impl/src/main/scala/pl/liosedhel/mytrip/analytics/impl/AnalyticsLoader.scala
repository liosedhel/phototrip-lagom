package pl.liosedhel.mytrip.analytics.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import pl.liosedhel.mytrip.analytics.api.AnalyticsService
import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

import scala.collection.immutable

class AnalyticsLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AnalyticsApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AnalyticsApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[AnalyticsService])
}

abstract class AnalyticsApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with CassandraPersistenceComponents
  with LagomKafkaComponents
  with PubSubComponents
  with AhcWSComponents
  with CORSComponents {

  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  lazy val worldMapService = serviceClient.implement[WorldMapService]

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[AnalyticsService]({
    val analyticsRepository = wire[AnalyticsRepository]
    wire[AnalyticsServiceImpl]
  })

  override def jsonSerializerRegistry: JsonSerializerRegistry = new JsonSerializerRegistry {
    override def serializers: immutable.Seq[
      JsonSerializer[_]
    ] = Nil
  }
}
