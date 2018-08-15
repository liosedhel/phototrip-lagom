package pl.liosedhel.mytrip.worldmapstream.impl

import java.util.UUID
import scala.concurrent.Future

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.ServiceCall

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.PlaceAdded
import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import pl.liosedhel.mytrip.worldmapstream.api.WorldMapStreamService

class WorldMapStreamServiceImpl(worldMapService: WorldMapService) extends WorldMapStreamService {
  override def newPlacesOnMap: ServiceCall[NotUsed, Source[PlaceAdded, NotUsed]] =
    ServiceCall[NotUsed, Source[PlaceAdded, NotUsed]] { _ =>
      Future.successful(
        worldMapService
          .placeAddedTopic().subscribe//.withGroupId(UUID.randomUUID().toString)
          .atMostOnceSource
          .mapMaterializedValue(_ => NotUsed)
      )
    }
}
