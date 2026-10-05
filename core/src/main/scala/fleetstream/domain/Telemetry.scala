package fleetstream.domain

import java.time.Instant
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

final case class Telemetry(
    deviceId: String,
    ts: Instant,
    socPercent: Double,
    powerKw: Double,
    voltageV: Double,
    tempC: Double,
    seq: Long
)

sealed trait DecodeError
final case class JsonDecodeError(message: String) extends DecodeError

object TelemetryCodec {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .registerModule(new JavaTimeModule())

  def encode(t: Telemetry): Array[Byte] = {
    mapper.writeValueAsBytes(t)
  }

  def decode(bytes: Array[Byte]): Either[DecodeError, Telemetry] = {
    try {
      Right(mapper.readValue(bytes, classOf[Telemetry]))
    } catch {
      case e: Exception => Left(JsonDecodeError(e.getMessage))
    }
  }
}
