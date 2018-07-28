package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{Coordinates, Url, WorldMap}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapCommands.{AddPlace, CreateNewMap, WorldMapCommand}
import pl.liosedhel.mytrip.worldmap.impl.WorldMapEvents.{PlaceAdded, WorldMapCreated, WorldMapEvent}

class WorldMapAggregateSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system =
    ActorSystem("WolrdMapAggreateSpec", JsonSerializerRegistry.actorSystemSetupFor(WorldMapSerializerRegistry))

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  private def withTestDriver(entityId: String)(
    block: String => PersistentEntityTestDriver[WorldMapCommand[_], WorldMapEvent, WorldMap] => Unit
  ): Unit = {
    val driver = new PersistentEntityTestDriver(system, new WorldMapAggregate, entityId)
    block(entityId)(driver)
    driver.getAllIssues should have size 0
  }

  "world map aggregate" should {



    "create even empty map" in withTestDriver("mapId1") { mapId => driver =>
      val creatorId = "creatorId"
      val outcome = driver.run(CreateNewMap(mapId, creatorId))
      outcome.replies should contain only Done
      outcome.events should contain only WorldMapCreated(mapId, creatorId)
    }

    "allow adding new places" in withTestDriver("mapId2") { mapId => driver =>
      val coordinates = Coordinates("15", "15")
      val urls        = Set(Url("Url"))

      val creatorId = "creatorId"
      val outcome = driver.run(CreateNewMap(mapId, creatorId))
      outcome.replies should contain only Done
      outcome.events should contain only WorldMapCreated(mapId, creatorId)

      val outcome2 = driver.run(AddPlace(coordinates, urls))
      println(outcome2.replies)
      //outcome2.replies should contain only Done
      outcome2.events should contain only PlaceAdded(mapId, coordinates, urls)
    }

  }
}
