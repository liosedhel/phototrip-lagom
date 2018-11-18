package pl.liosedhel.mytrip.analytics.impl
import java.util.UUID

import akka.stream.scaladsl.Flow
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import pl.liosedhel.mytrip.analytics.api.{AnalyticsService, Stats}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.{PlaceAdded, WorldMapCreated}
import pl.liosedhel.mytrip.worldmap.api.WorldMapService

import scala.concurrent.Future

class AnalyticsServiceImpl(
  analyticsRepository: AnalyticsRepository,
  worldMapService: WorldMapService
) extends AnalyticsService {

  worldMapService
    .placeCreatedTopic()
    .subscribe
    .withGroupId(UUID.randomUUID().toString) //TODO: demonstration only, just to fully restore in memory repository
    .atLeastOnce(
      Flow[PlaceAdded].map { placeAdded =>
        analyticsRepository.save(placeAdded)
        Done
      }
    )

  worldMapService
    .worldMapCreatedTopic()
    .subscribe
    .withGroupId(UUID.randomUUID().toString) //TODO: demonstration only, just to fully restore in memory repository
    .atLeastOnce(
      Flow[WorldMapCreated].map { worldMapCreated =>
        analyticsRepository.save(worldMapCreated)
        Done
      }
    )

  override def getStats(): ServiceCall[NotUsed, Stats] =
    ServiceCall[NotUsed, Stats] { _ =>
      Future.successful(analyticsRepository.getStats())
    }

}
