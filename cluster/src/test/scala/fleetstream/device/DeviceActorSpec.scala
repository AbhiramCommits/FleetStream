package fleetstream.device

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import fleetstream.domain.Command
import fleetstream.registry.InMemoryRepository
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeviceActorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  val testKit = ActorTestKit()

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "DeviceActor" should {
    "handle setpoint command and update state" in {
      val repo = new InMemoryRepository()
      val actor = testKit.spawn(DeviceActor("dev-test", repo))
      val probe = testKit.createTestProbe[DeviceActor.CommandAckResponse]()

      val cmd = Command.SetPower("dev-test", 50.0, "cmd-1", java.time.Instant.now())
      actor ! DeviceActor.Apply(cmd, probe.ref)

      probe.expectMessage(DeviceActor.CommandDispatched("cmd-1"))

      val stateProbe = testKit.createTestProbe[DeviceActor.DeviceState]()
      actor ! DeviceActor.GetState(stateProbe.ref)
      val state = stateProbe.receiveMessage()

      state.currentSetpoint shouldBe Some(50.0)
    }
  }
}
