package fleetstream.registry

import fleetstream.domain.CommandAck
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

case class DeviceRecord(deviceId: String, siteId: String, model: String, ratedKw: Double, registeredAt: Instant)
case class CommandAuditRecord(commandId: String, deviceId: String, kind: String, payload: String, issuedAt: Instant, ackedAt: Option[Instant], attempts: Int, status: String)

trait DeviceRepository {
  def saveDevice(device: DeviceRecord): Unit
  def getDevice(deviceId: String): Option[DeviceRecord]
  def saveCommand(audit: CommandAuditRecord): Unit
  def updateCommandStatus(commandId: String, status: String, ackedAt: Option[Instant], attempts: Int): Unit
  def getCommand(commandId: String): Option[CommandAuditRecord]
  def listCommands(deviceId: String): Seq[CommandAuditRecord]
}

class InMemoryRepository extends DeviceRepository {
  private val devices = new ConcurrentHashMap[String, DeviceRecord]()
  private val commands = new ConcurrentHashMap[String, CommandAuditRecord]()

  override def saveDevice(device: DeviceRecord): Unit = devices.put(device.deviceId, device)
  override def getDevice(deviceId: String): Option[DeviceRecord] = Option(devices.get(deviceId))

  override def saveCommand(audit: CommandAuditRecord): Unit = commands.put(audit.commandId, audit)

  override def updateCommandStatus(commandId: String, status: String, ackedAt: Option[Instant], attempts: Int): Unit = {
    Option(commands.get(commandId)).foreach { existing =>
      commands.put(commandId, existing.copy(status = status, ackedAt = ackedAt, attempts = attempts))
    }
  }

  override def getCommand(commandId: String): Option[CommandAuditRecord] = Option(commands.get(commandId))
  override def listCommands(deviceId: String): Seq[CommandAuditRecord] = {
    commands.values().asScala.filter(_.deviceId == deviceId).toSeq
  }
}
