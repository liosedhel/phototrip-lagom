package pl.liosedhel.mytrip.worldmapstream.impl

import scala.concurrent.Future

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.ServiceCall

import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import pl.liosedhel.mytrip.worldmapstream.api.{Coordinates, NewPlaceOnMap, Url, WorldMapStreamService}

class WorldMapStreamServiceImpl(worldMapService: WorldMapService) extends WorldMapStreamService {

  override def newPlacesOnMap: ServiceCall[NotUsed, Source[NewPlaceOnMap, NotUsed]] =
    ServiceCall[NotUsed, Source[NewPlaceOnMap, NotUsed]] { _ =>
      Future.successful(
        worldMapService
          .placeAddedTopic()
          .subscribe //.withGroupId(UUID.randomUUID().toString)
          .atMostOnceSource
          .map { placeAdded =>
            NewPlaceOnMap(
              placeAdded.placeId,
              Coordinates(placeAdded.coordinates.latitude, placeAdded.coordinates.longitude),
              placeAdded.photoLinks.map(link => Url(link.url))
            )
          }
          .mapMaterializedValue(_ => NotUsed)
      )
    }

}
