package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import org.slf4j.{Logger, LoggerFactory}
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate._

class PlaceEventProcessor(
  placesRepository: PlacesRepository
) extends ReadSideProcessor[PlaceEvent] {

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[PlaceEventProcessor])

  //building view side for places
  //only for demonstration purpose, we are not dealing here with offset so each time on startup all events will be processed
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[PlaceEvent] = { () =>
    Flow
      .apply[EventStreamElement[PlaceEvent]]
      .collect {
        case EventStreamElement(_, event: PlaceCreated, _) =>
          log.info(s"New place was created $event")
          placesRepository.savePlace(event); Done
        case EventStreamElement(_, event: LinkAdded, _)    => placesRepository.linkAdded(event); Done
      }
  }

  override def aggregateTags = Set(PlaceEvent.Tag)

}
