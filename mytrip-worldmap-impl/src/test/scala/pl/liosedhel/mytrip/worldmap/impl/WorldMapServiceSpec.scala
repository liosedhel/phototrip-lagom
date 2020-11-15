package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._
import pl.liosedhel.mytrip.worldmap.api.WorldMapService
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import pl.liosedhel.mytrip.worldmap.api.PlaceId
import pl.liosedhel.mytrip.worldmap.api.WorldMapId
import pl.liosedhel.mytrip.user.api.UserId

class WorldMapServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new WorldMapApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[WorldMapService]

  override protected def afterAll() = server.stop()

  "world map service" should {

    "add map" in {
      val map1 = "map1"
      client
        .createPlace("mapId")
        .invoke(Place(PlaceId("placeId"), "dsfsdf", Coordinates("123123", "123123"), Set(Url("wwww"))))
        .map { answer =>
          answer should ===(Done)
        }
    }

    "allow adding place to map" in {
      val place     = Place(PlaceId("descriptionId"), "description", Coordinates("15", "16"), Set(Url("url")))
      val mapId     = WorldMapId("map2")
      val creatorId = UserId("creator1")
      for {
        _      <- client.createWorldMap().invoke(WorldMapDetails(mapId, creatorId, None))
        answer <- client.createPlace(mapId.id).invoke(place)
        map1   <- client.worldMap(mapId.id).invoke()
        allMaps <- client
          .availableMaps()
          .invoke() // TODO: find reliable way to test this (read side is updated with some delay)
      } yield {
        answer should ===(Done)
        //allMaps.maps.size should ===(1)
        //allMaps.maps.head should ===(mapId)
        map1 should ===(WorldMap(mapId, creatorId, Some("description"), Set(place)))
      }
    }
  }
}
