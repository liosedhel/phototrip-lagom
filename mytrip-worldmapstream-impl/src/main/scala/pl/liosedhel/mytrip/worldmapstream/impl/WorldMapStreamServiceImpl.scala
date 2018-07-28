package pl.liosedhel.mytrip.worldmapstream.impl

import scala.concurrent.Future

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.ServiceCall

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.PlaceAdded
import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import pl.liosedhel.mytrip.worldmapstream.api.WorldMapStreamService

class WorldMapStreamServiceImpl(worldMapService: WorldMapService) extends WorldMapStreamService {
  override def newPlacesOnMap(mapId: String): ServiceCall[Source[String, NotUsed], Source[PlaceAdded, NotUsed]] =
    ServiceCall[Source[String, NotUsed], Source[PlaceAdded, NotUsed]] { _ =>
      Future.successful(
        worldMapService
          .placeAddedTopic()
          .subscribe
          .atMostOnceSource
          .filter(_.worldMapId == mapId)
          .mapMaterializedValue(_ => NotUsed)
      )
    }
}
