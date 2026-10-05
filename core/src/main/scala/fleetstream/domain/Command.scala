package fleetstream.domain

import java.time.Instant

sealed trait Command {
  def deviceId: String
  def commandId: String
  def issuedAt: Instant
}

object Command {
  final case class SetPower(
      deviceId: String,
      targetKw: Double,
      commandId: String,
      issuedAt: Instant
  ) extends Command

  final case class SetMode(
      deviceId: String,
      mode: String,
      commandId: String,
      issuedAt: Instant
  ) extends Command
}

final case class CommandAck(
    commandId: String,
    deviceId: String,
    accepted: Boolean,
    ackedAt: Instant
)
