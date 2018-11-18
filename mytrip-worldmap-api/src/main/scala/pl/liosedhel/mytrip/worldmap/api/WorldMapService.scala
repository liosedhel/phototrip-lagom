package pl.liosedhel.mytrip.worldmap.api

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.{PlaceAdded, WorldMapCreated}
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiFormatters._
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._
import play.api.libs.json._

object WorldMapService {
  val WORLD_MAP_CREATED = "world-map-created"
  val PLACE_ADDED       = "place-added"
}

trait WorldMapService extends Service {

  def worldMap(mapId: String): ServiceCall[NotUsed, WorldMap]

  def createWorldMap(): ServiceCall[WorldMapDetails, Done]

  def availableMaps(): ServiceCall[NotUsed, AvailableMaps]

  def createPlace(mapId: String): ServiceCall[Place, Done]

  def addLink(placeId: String): ServiceCall[Url, Done]

  def placeAddedStream(mapId: String): ServiceCall[NotUsed, Source[WorldMapApiModel.Place, NotUsed]]

  //topics available externally
  def worldMapCreatedTopic(): Topic[WorldMapCreated]

  def placeCreatedTopic(): Topic[PlaceAdded]

  override def descriptor: Descriptor = {
    import Service._
    named("worldmap")
      .withCalls(
        pathCall("/api/world-map/map/:id", worldMap _),
        pathCall("/api/world-map/map", createWorldMap _),
        pathCall("/api/world-map/map/:id/place", createPlace _),
        pathCall("/api/world-map/map/list/available", availableMaps _),
        pathCall("/api/world-map/map/:id/stream/places", placeAddedStream _),
        pathCall("/api/world-map/place/:id/link", addLink _)
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
            PartitionKeyStrategy[WorldMapCreated](_.worldMapId.id)
          ),
        topic(WorldMapService.PLACE_ADDED, placeCreatedTopic())
        // Kafka partitions messages, messages within the same partition will
        // be delivered in order, to ensure that all messages for the same user
        // go to the same partition (and hence are delivered in order with respect
        // to that user), we configure a partition key strategy that extracts the
        // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[PlaceAdded](_.worldMapId.id)
          )
      )
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "/api.*")
      )
  }
}

case class WorldMapId(id: String) extends AnyVal
object WorldMapId {
  implicit val worldMapIdFormat = new Format[WorldMapId] {
    override def writes(
      o: WorldMapId
    ): JsValue =
      Json.toJson(o.id)
    override def reads(
      json: JsValue
    ): JsResult[WorldMapId] =
      json.validate(implicitly[Reads[String]]).map(apply)
  }
}

case class PlaceId(id: String) extends AnyVal
object PlaceId {
  implicit val worldMapIdFormat = new Format[PlaceId] {
    override def writes(
      o: PlaceId
    ): JsValue =
      Json.toJson(o.id)
    override def reads(
      json: JsValue
    ): JsResult[PlaceId] =
      json.validate(implicitly[Reads[String]]).map(apply)
  }
}

object WorldMapApiModel {

  case class Url(url: String) extends AnyVal
  case class Coordinates(latitude: String, longitude: String)
  case class Place(placeId: PlaceId, description: String, coordinates: Coordinates, photoLinks: Set[Url])
  case class WorldMap(mapId: WorldMapId, creatorId: String, description: Option[String], places: Set[Place])

  case class AvailableMaps(maps: Seq[WorldMapDetails])
  case class WorldMapDetails(mapId: WorldMapId, creatorId: String, description: Option[String])
}

object WorldMapApiEvents {
  case class WorldMapCreated(worldMapId: WorldMapId, creatorId: String, description: String)
  case class PlaceAdded(placeId: PlaceId, worldMapId: WorldMapId, description: String, coordinates: Coordinates, photoLinks: Set[Url])
}

object WorldMapApiFormatters {

  implicit val urlFormat: Format[Url] = Json.format[Url]

  implicit val coordinatesFormat: Format[Coordinates] = Json.format[Coordinates]
  implicit val placeFormat: Format[Place]             = Json.format[Place]
  implicit val worldMapFormat: Format[WorldMap]       = Json.format[WorldMap]

  implicit val newWorldMapFormat: Format[WorldMapDetails] = Json.format[WorldMapDetails]
  implicit val availableMapsFormat: Format[AvailableMaps] = Json.format[AvailableMaps]

  implicit val worldMapCreatedFormat: Format[WorldMapCreated] = Json.format[WorldMapCreated]
  implicit val placeAddedFormat: Format[PlaceAdded]           = Json.format[PlaceAdded]
}
