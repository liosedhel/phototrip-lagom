package pl.liosedhel.mytrip.worldmapstream.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import pl.liosedhel.mytrip.worldmap.api.PlaceId
import play.api.libs.json.{Format, Json}

trait WorldMapStreamService extends Service {

  import JsonFormatters._

  def newPlacesOnMap: ServiceCall[NotUsed, Source[NewPlaceOnMap, NotUsed]]

  override final def descriptor = {
    import Service._

    named("mytrip-worldmapstream")
      .withCalls(
        pathCall("/api/worldmapstream/places/stream", newPlacesOnMap)
      )
      .withAutoAcl(true)
  }

}

case class Url(url: String) extends AnyVal
case class Coordinates(latitude: String, longitude: String)
case class NewPlaceOnMap(id: PlaceId, coordinates: Coordinates, photoLinks: Set[Url])

object JsonFormatters {
  implicit val urlFormat: Format[Url]                     = Json.format[Url]
  implicit val coordinatesFormat: Format[Coordinates]     = Json.format[Coordinates]
  implicit val newPlaceOnMapFormat: Format[NewPlaceOnMap] = Json.format[NewPlaceOnMap]
}
