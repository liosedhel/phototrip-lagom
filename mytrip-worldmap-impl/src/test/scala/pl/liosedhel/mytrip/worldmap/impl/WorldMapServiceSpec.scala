package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel._
import pl.liosedhel.mytrip.worldmap.api.WorldMapService

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
      client.createWorldMap(map1).invoke(NewWorldMap("creatorId")).map { answer =>
        answer should ===(Done)
      }
    }

    "allow adding place to map" in {
      val place     = Place(Coordinates("15", "16"), Set(Url("url")))
      val mapId     = "map2"
      val creatorId = "creator1"
      for {
        _       <- client.createWorldMap(mapId).invoke(NewWorldMap(creatorId))
        answer  <- client.addPlace(mapId).invoke(place)
        map1    <- client.worldMap(mapId).invoke()
        //allMaps <- client.availableMaps().invoke() // TODO: find reliable way to test this (read side is updated with some delay)
      } yield {
        answer should ===(Done)
        //allMaps.maps.size should ===(1)
        //allMaps.maps.head should ===(mapId)
        map1 should ===(WorldMap(mapId, creatorId, List(place)))
      }
    }
  }
}
