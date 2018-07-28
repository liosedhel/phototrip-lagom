package pl.liosedhel.mytrip.worldmapstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

import pl.liosedhel.mytrip.worldmap.api.WorldMapService
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
  with AhcWSComponents {

  lazy val worldMapService = serviceClient.implement[WorldMapService]

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[WorldMapStreamService](wire[WorldMapStreamServiceImpl])

}
