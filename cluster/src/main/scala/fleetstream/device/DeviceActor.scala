package fleetstream.device

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import fleetstream.domain.{Aggregate, Command, CommandAck}
import fleetstream.registry.DeviceRepository

object DeviceActor {

  sealed trait CommandMessage

  val EntityTypeKey: akka.cluster.sharding.typed.scaladsl.EntityTypeKey[CommandMessage] =
    akka.cluster.sharding.typed.scaladsl.EntityTypeKey[CommandMessage]("DeviceActor")

  final case class Ingest(aggregate: Aggregate, replyTo: Option[ActorRef[AckResponse]] = None) extends CommandMessage
  final case class Apply(command: Command, replyTo: ActorRef[CommandAckResponse]) extends CommandMessage
  final case class Ack(ack: CommandAck) extends CommandMessage
  final case class GetState(replyTo: ActorRef[DeviceState]) extends CommandMessage

  sealed trait AckResponse
  case object Accepted extends AckResponse

  sealed trait CommandAckResponse
  final case class CommandDispatched(commandId: String) extends CommandAckResponse
  final case class CommandAlreadyProcessed(commandId: String) extends CommandAckResponse

  final case class DeviceState(
      deviceId: String,
      lastAggregate: Option[Aggregate],
      currentSetpoint: Option[Double],
      mode: String,
      pendingCommands: Map[String, Command]
  )

  def apply(deviceId: String, repo: DeviceRepository): Behavior[CommandMessage] = {
    active(deviceId, repo, None, None, "normal", Map.empty)
  }

  private def active(
      deviceId: String,
      repo: DeviceRepository,
      lastAggregate: Option[Aggregate],
      currentSetpoint: Option[Double],
      mode: String,
      pendingCommands: Map[String, Command]
  ): Behavior[CommandMessage] = {
    Behaviors.receive { (context, msg) =>
      msg match {
        case Ingest(agg, replyTo) =>
          replyTo.foreach(_ ! Accepted)
          active(deviceId, repo, Some(agg), currentSetpoint, mode, pendingCommands)

        case Apply(cmd, replyTo) =>
          cmd match {
            case Command.SetPower(_, targetKw, commandId, _) =>
              replyTo ! CommandDispatched(commandId)
              active(deviceId, repo, lastAggregate, Some(targetKw), mode, pendingCommands + (commandId -> cmd))
            case Command.SetMode(_, newMode, commandId, _) =>
              replyTo ! CommandDispatched(commandId)
              active(deviceId, repo, lastAggregate, currentSetpoint, newMode, pendingCommands + (commandId -> cmd))
          }

        case Ack(ack) =>
          val updatedPending = pendingCommands - ack.commandId
          repo.updateCommandStatus(ack.commandId, if (ack.accepted) "acked" else "failed", Some(ack.ackedAt), 1)
          active(deviceId, repo, lastAggregate, currentSetpoint, mode, updatedPending)

        case GetState(replyTo) =>
          replyTo ! DeviceState(deviceId, lastAggregate, currentSetpoint, mode, pendingCommands)
          Behaviors.same
      }
    }
  }
}
