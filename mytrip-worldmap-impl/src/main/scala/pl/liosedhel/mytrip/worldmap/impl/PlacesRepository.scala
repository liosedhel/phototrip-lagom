package pl.liosedhel.mytrip.worldmap.impl
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{Coordinates, Url}
import pl.liosedhel.mytrip.worldmap.api.{PlaceId, WorldMapApiModel, WorldMapId}
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate.{LinkAdded, PlaceCreated}

import scala.concurrent.Future

/**
 * In memory storage, for demonstration only
 */
class PlacesRepository {

  case class Place(
    placeId: PlaceId,
    mapId: WorldMapId,
    description: String,
    coordinates: Coordinates,
    photoLinks: Set[Url]
  )
  var places = Map.empty[PlaceId, Place]

  def savePlace(placeCreated: PlaceCreated): Unit = {
    import placeCreated._
    places = places + (placeId -> Place(placeId, mapId, description, coordinates, photoLinks))
  }

  def linkAdded(linkAdded: LinkAdded): Unit = {
    places
      .get(linkAdded.placeId)
      .map(p => p.copy(photoLinks = p.photoLinks + linkAdded.url))
      .foreach(p => places = places + (linkAdded.placeId -> p))
  }

  def getPlaces(mapId: WorldMapId): Future[Set[WorldMapApiModel.Place]] = Future.successful {
    places.values
      .filter(_.mapId == mapId)
      .map(place => WorldMapApiModel.Place(place.placeId, place.description, place.coordinates, place.photoLinks))
      .toSet
  }

}
