package fleetstream.domain

import java.time.Instant

final case class WindowKey(deviceId: String, windowStart: Instant)

final case class Aggregate(
    key: WindowKey,
    count: Long,
    meanPowerKw: Double,
    maxPowerKw: Double,
    minSocPercent: Double,
    maxTempC: Double
)

object Aggregate {
  def from(window: Seq[Telemetry]): Aggregate = {
    require(window.nonEmpty, "Cannot aggregate empty window of telemetry")
    val deviceId = window.head.deviceId
    val windowStart = window.head.ts // simplified windowing start
    val count = window.size.toLong
    val sumPower = window.map(_.powerKw).sum
    val meanPowerKw = sumPower / count
    val maxPowerKw = window.map(_.powerKw).max
    val minSocPercent = window.map(_.socPercent).min
    val maxTempC = window.map(_.tempC).max

    Aggregate(
      WindowKey(deviceId, windowStart),
      count,
      meanPowerKw,
      maxPowerKw,
      minSocPercent,
      maxTempC
    )
  }

  def merge(a: Aggregate, b: Aggregate): Aggregate = {
    require(a.key == b.key, "Cannot merge aggregates with different window keys")
    val totalCount = a.count + b.count
    if (totalCount == 0) a
    else {
      val meanPower = ((a.meanPowerKw * a.count) + (b.meanPowerKw * b.count)) / totalCount.toDouble
      val maxPower = math.max(a.maxPowerKw, b.maxPowerKw)
      val minSoc = math.min(a.minSocPercent, b.minSocPercent)
      val maxTemp = math.max(a.maxTempC, b.maxTempC)
      Aggregate(
        a.key,
        totalCount,
        meanPower,
        maxPower,
        minSoc,
        maxTemp
      )
    }
  }
}
