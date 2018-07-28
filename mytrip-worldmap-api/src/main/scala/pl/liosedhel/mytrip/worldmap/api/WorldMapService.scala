package pl.liosedhel.mytrip.worldmap.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.{PlaceAdded, WorldMapCreated}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters._
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._

object WorldMapService {
  val WORLD_MAP_CREATED = "world-map-created"
  val PLACE_ADDED       = "place-added"
}

trait WorldMapService extends Service {

  def worldMap(id: String): ServiceCall[NotUsed, WorldMap]

  def createWorldMap(id: String): ServiceCall[NewWorldMap, Done]

  def availableMaps(): ServiceCall[NotUsed, AvailableMaps]

  def addPlace(id: String): ServiceCall[Place, Done]

  //topics
  def worldMapCreatedTopic(): Topic[WorldMapCreated]

  def placeAddedTopic(): Topic[PlaceAdded]

  override def descriptor: Descriptor = {
    import Service._
    named("worldmap")
      .withCalls(
        pathCall("/api/world-map/:id", worldMap _),
        pathCall("/api/world-map/:id", createWorldMap _),
        pathCall("/api/world-map/:id/place", addPlace _),
        pathCall("/api/world-map/list/available", availableMaps _)
      )
      .withTopics(
        topic(WorldMapService.WORLD_MAP_CREATED, worldMapCreatedTopic())
        // Kafka partitions messages, messages within the same partition will
        // be delivered in order, to ensure that all messages for the same user
        // go to the same partition (and hence are delivered in order with respect
        // to that user), we configure a partition key strategy that extracts the
        // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[WorldMapCreated](_.id)
          ),
        topic(WorldMapService.PLACE_ADDED, placeAddedTopic())
        // Kafka partitions messages, messages within the same partition will
        // be delivered in order, to ensure that all messages for the same user
        // go to the same partition (and hence are delivered in order with respect
        // to that user), we configure a partition key strategy that extracts the
        // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[PlaceAdded](_.worldMapId)
          )
      )
      .withAutoAcl(true)
  }
}

object WorldMapApiModel {
  case class Url(url: String) extends AnyVal
  case class Coordinates(latitude: String, longitude: String)
  case class Place(coordinates: Coordinates, photoLinks: Set[Url])
  case class WorldMap(id: String, creatorId: String, places: List[Place])

  case class AvailableMaps(maps: Seq[String])
  case class NewWorldMap(creatorId: String)
}

object WorldMapApiEvents {
  case class WorldMapCreated(id: String, creatorId: String)
  case class PlaceAdded(worldMapId: String, coordinates: Coordinates, photoLinks: Set[Url])
}

object WorldMapApiFormatters {
  implicit val urlFormat: Format[Url]                 = Json.format[Url]
  implicit val coordinatesFormat: Format[Coordinates] = Json.format[Coordinates]
  implicit val placeFormat: Format[Place]             = Json.format[Place]
  implicit val worldMapFormat: Format[WorldMap]       = Json.format[WorldMap]

  implicit val availableMapsFormat: Format[AvailableMaps] = Json.format[AvailableMaps]
  implicit val newWorldMapFormat: Format[NewWorldMap]     = Json.format[NewWorldMap]

  implicit val worldMapCreatedFormat: Format[WorldMapCreated] = Json.format[WorldMapCreated]
  implicit val placeAddedFormat: Format[PlaceAdded]           = Json.format[PlaceAdded]
}
