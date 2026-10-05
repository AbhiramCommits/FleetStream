package fleetstream.http

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import fleetstream.device.DeviceActor
import fleetstream.domain.Command
import fleetstream.registry.DeviceRepository
import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.Future

class ControlApi(sharding: ClusterSharding, repo: DeviceRepository)(implicit system: ActorSystem[_], ec: ExecutionContext) {
  implicit val timeout: Timeout = 3.seconds

  val routes: Route =
    concat(
      path("health") {
        get {
          complete("OK")
        }
      },
      path("metrics") {
        get {
          complete("# HELP fleetstream_uptime_seconds Uptime\n# TYPE fleetstream_uptime_seconds counter\nfleetstream_uptime_seconds 42\n")
        }
      },
      path("devices" / Segment / "setpoint") { deviceId =>
        post {
          parameters("targetKw".as[Double]) { targetKw =>
            val commandId = UUID.randomUUID().toString
            val cmd = Command.SetPower(deviceId, targetKw, commandId, Instant.now())
            val entityRef = sharding.entityRefFor(DeviceActor.EntityTypeKey, deviceId)
            val futureRes: Future[DeviceActor.CommandAckResponse] = entityRef.ask(ref => DeviceActor.Apply(cmd, ref))

            onSuccess(futureRes) { res =>
              res match {
                case DeviceActor.CommandDispatched(id) =>
                  complete(StatusCodes.Accepted, s"""{"commandId":"$id","status":"dispatched"}""")
                case DeviceActor.CommandAlreadyProcessed(id) =>
                  complete(StatusCodes.OK, s"""{"commandId":"$id","status":"already_processed"}""")
              }
            }
          }
        }
      },
      path("devices" / Segment) { deviceId =>
        get {
          val entityRef = sharding.entityRefFor(DeviceActor.EntityTypeKey, deviceId)
          val futureState: Future[DeviceActor.DeviceState] = entityRef.ask(ref => DeviceActor.GetState(ref))
          onSuccess(futureState) { state =>
            complete(s"""{"deviceId":"${state.deviceId}","mode":"${state.mode}","currentSetpoint":${state.currentSetpoint.getOrElse(0.0)}}""")
          }
        }
      },
      path("devices" / Segment / "commands") { deviceId =>
        get {
          val commands = repo.listCommands(deviceId)
          val json = commands.map(c => s"""{"commandId":"${c.commandId}","status":"${c.status}"}""").mkString("[", ",", "]")
          complete(json)
        }
      }
    )
}
