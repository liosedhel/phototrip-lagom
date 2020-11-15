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
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate.PlaceCommand
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate.PlaceEvent
import pl.liosedhel.mytrip.worldmap.impl.PlaceAggregate.PlaceState
import pl.liosedhel.mytrip.worldmap.api.WorldMapApiEvents.PlaceAdded

class PlaceAggregateSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private val system =
    ActorSystem("PlaceAggreagateSpec", JsonSerializerRegistry.actorSystemSetupFor(WorldMapSerializerRegistry))

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  private def withTestDriver(entityId: WorldMapId)(
    block: WorldMapId => PersistentEntityTestDriver[PlaceCommand[_], PlaceEvent, PlaceState] => Unit
  ): Unit = {
    val driver = new PersistentEntityTestDriver(system, new PlaceAggregate(null), entityId.id)
    block(entityId)(driver)
    driver.getAllIssues should have size 0
  }

  "place aggreagate should" should {

    "allow adding new places" in withTestDriver(WorldMapId(("mapId2"))) { mapId => driver =>
      val coordinates = Coordinates("15", "15")
      val urls        = Set(Url("Url"))

      val placeId  = PlaceId("placeId")
      val outcome2 = driver.run(CreateNewPlace(placeId, mapId, "description", coordinates, urls))
      println(outcome2.replies)
      //outcome2.replies should contain only Done
      outcome2.events should contain only PlaceAdded(placeId, mapId, "description", coordinates, urls)
    }

  }
}
