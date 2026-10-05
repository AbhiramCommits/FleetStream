package fleetstream.ingest

import scala.concurrent.duration.FiniteDuration

final case class IngestConfig(
    maxSubstreams: Int,
    windowDuration: FiniteDuration,
    batchSize: Int,
    bufferSize: Int,
    overflowStrategy: akka.stream.OverflowStrategy
)

final case class IngestStat(
    validCount: Long,
    rejectedCount: Long
)
