package pl.liosedhel.mytrip.worldmap.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

import pl.liosedhel.mytrip.worldmap.api.WorldMapService

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
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[WorldMapService]({
    val worldMapsRepository = wire[WorldMapsRepository]
    wire[WorldMapServiceImpl]
  })

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = WorldMapSerializerRegistry

  // Register world map aggregate
  persistentEntityRegistry.register(wire[WorldMapAggregate])
}
