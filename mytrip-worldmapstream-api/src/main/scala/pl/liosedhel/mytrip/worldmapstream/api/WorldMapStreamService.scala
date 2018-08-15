package pl.liosedhel.mytrip.worldmapstream.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.PlaceAdded

trait WorldMapStreamService extends Service {

  import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters._

  def newPlacesOnMap: ServiceCall[NotUsed, Source[PlaceAdded, NotUsed]]

  override final def descriptor = {
    import Service._

    named("mytrip-worldmapstream")
      .withCalls(
        pathCall("/api/worldmapstream/places/stream", newPlacesOnMap)
      ).withAutoAcl(true)
  }
}
