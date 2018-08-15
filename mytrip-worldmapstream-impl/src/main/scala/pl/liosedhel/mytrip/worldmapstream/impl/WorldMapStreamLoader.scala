package pl.liosedhel.mytrip.worldmapstream.impl

import scala.collection.immutable

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.PlaceAdded
import pl.liosedhel.mytrip.worldmap.api.{WorldMapApiFormatters, WorldMapService}
import pl.liosedhel.mytrip.worldmapstream.api.WorldMapStreamService

class WorldMapStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new MyTripStreamApplication(context) {
      override def serviceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MyTripStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[WorldMapStreamService])
}

abstract class MyTripStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with AhcWSComponents with LagomKafkaComponents with CassandraPersistenceComponents {

  lazy val worldMapService = serviceClient.implement[WorldMapService]

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[WorldMapStreamService](wire[WorldMapStreamServiceImpl])

  override lazy val jsonSerializerRegistry = new JsonSerializerRegistry {
    import WorldMapApiFormatters._
    override def serializers: immutable.Seq[
      JsonSerializer[_]
    ] = {
      immutable.Seq(JsonSerializer[PlaceAdded])
    }
  }

}
