package pl.liosedhel.mytrip.analytics.impl
import pl.liosedhel.mytrip.analytics.api.Stats
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.{PlaceAdded, WorldMapCreated}

class AnalyticsRepository {

  var stats = Stats(0, 0)

  def save(placeAdded: PlaceAdded): Unit =
    stats = stats.copy(placesNumber = stats.placesNumber + 1)

  def save(mapCreated: WorldMapCreated): Unit =
    stats = stats.copy(mapsNumber = stats.mapsNumber + 1)

  def getStats(): Stats =
    stats
}
