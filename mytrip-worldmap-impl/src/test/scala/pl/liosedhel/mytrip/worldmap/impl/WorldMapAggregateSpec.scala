package pl.liosedhel.mytrip.worldmap.impl

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver

import pl.liosedhel.mytrip.worldmap.api.WorldMapApiModel.{Coordinates, Url, WorldMap}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.wordspec.AnyWordSpec
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.WorldMapCommand
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.WorldMapEvent
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.CreateNewMap
import pl.liosedhel.mytrip.user.api.UserId
import pl.liosedhel.mytrip.worldmap.api.WorldMapId
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.WorldMapCreated
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate.CreateNewPlace
import pl.liosedhel.mytrip.worldmap.api.PlaceId
import pl.liosedhel.mytrip.worldmap.impl.WorldMapAggregate.WorldMapState

class WorldMapAggregateSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private val system =
    ActorSystem("WolrdMapAggreateSpec", JsonSerializerRegistry.actorSystemSetupFor(WorldMapSerializerRegistry))

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  private def withTestDriver(entityId: WorldMapId)(
    block: WorldMapId => PersistentEntityTestDriver[WorldMapCommand[_], WorldMapEvent, WorldMapState] => Unit
  ): Unit = {
    val driver = new PersistentEntityTestDriver(system, new WorldMapAggregate(null), entityId.id)
    block(entityId)(driver)
    driver.getAllIssues should have size 0
  }

  "world map aggregate" should {

    "create even empty map" in withTestDriver(WorldMapId("mapId1")) { mapId => driver =>
      val creatorId = UserId("creatorId")
      val outcome   = driver.run(CreateNewMap(mapId, creatorId, "description"))
      outcome.replies should contain only Done
      outcome.events should contain only WorldMapCreated(mapId, creatorId, "description")
    }

  }
}
