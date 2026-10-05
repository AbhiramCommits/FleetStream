package fleetstream.ingest

import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink}
import fleetstream.domain.{Aggregate, Telemetry, TelemetryCodec}
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.Future

object IngestPipeline {

  def apply(
      cfg: IngestConfig,
      sink: Sink[Seq[Aggregate], Future[Done]]
  ): Flow[Array[Byte], IngestStat, NotUsed] = {
    val validCounter = new AtomicLong(0L)
    val rejectedCounter = new AtomicLong(0L)

    val flow: Flow[Array[Byte], IngestStat, NotUsed] = Flow[Array[Byte]]
      .map(bytes => TelemetryCodec.decode(bytes))
      .map {
        case Right(t) =>
          validCounter.incrementAndGet()
          Some(t)
        case Left(_) =>
          rejectedCounter.incrementAndGet()
          None
      }
      .collect { case Some(t) => t }
      .groupBy(cfg.maxSubstreams, (t: Telemetry) => t.deviceId)
      .groupedWithin(Int.MaxValue, cfg.windowDuration)
      .map(chunk => Aggregate.from(chunk))
      .mergeSubstreams
      .grouped(cfg.batchSize)
      .alsoTo(sink)
      .map(_ => IngestStat(validCounter.get(), rejectedCounter.get()))

    flow
  }
}
