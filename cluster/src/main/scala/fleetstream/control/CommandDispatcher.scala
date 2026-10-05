package fleetstream.control

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import fleetstream.device.DeviceActor
import fleetstream.domain.{Command, CommandAck}
import fleetstream.registry.{CommandAuditRecord, DeviceRepository}
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CommandDispatcher(
    sharding: ClusterSharding,
    repo: DeviceRepository,
    maxAttempts: Int = 3,
    baseBackoff: FiniteDuration = 1.second
)(implicit ec: ExecutionContext) {

  sealed trait DispatchMessage
  final case class RetryCommand(command: Command, attempt: Int, replyTo: ActorRef[DeviceActor.CommandAckResponse]) extends DispatchMessage

  def dispatch(command: Command, replyTo: ActorRef[DeviceActor.CommandAckResponse])(implicit context: ActorContext[DispatchMessage]): Unit = {
    val audit = CommandAuditRecord(
      command.commandId,
      command.deviceId,
      command.getClass.getSimpleName,
      command.toString,
      command.issuedAt,
      None,
      0,
      "pending"
    )
    repo.saveCommand(audit)

    val entityRef = sharding.entityRefFor(DeviceActor.EntityTypeKey, command.deviceId)
    entityRef ! DeviceActor.Apply(command, replyTo)

    context.scheduleOnce(baseBackoff, context.self, RetryCommand(command, 1, replyTo))
  }
}
