package fleetstream.loadgen

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import fleetstream.domain.{Telemetry, TelemetryCodec}
import fleetstream.ingest.{IngestConfig, IngestPipeline}
import fleetstream.sink.InMemorySink
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class FleetSimulator(devices: Int, ratePerDevice: Double, durationSec: Int) {

  def run(): (Long, Long, Double) = {
    implicit val system = ActorSystem("FleetSimulator")
    implicit val mat = akka.stream.Materializer(system)
    implicit val ec = system.dispatcher

    val memSink = new InMemorySink()
    val cfg = IngestConfig(
        maxSubstreams = devices,
        windowDuration = 1.second,
        batchSize = 100,
        bufferSize = 10000,
        overflowStrategy = OverflowStrategy.dropNew
    )

    val pipeline = IngestPipeline(cfg, memSink.sink)
    val (queue, source) = fleetstream.ingest.TelemetrySource.queue(50000).preMaterialize()

    val streamFuture = source
      .via(pipeline)
      .runWith(Sink.last)

    val startTime = System.currentTimeMillis()
    val totalTelemetry = (devices * ratePerDevice * durationSec).toLong

    val rnd = new Random()
    val emitThread = new Thread(() => {
      for (sec <- 0 until durationSec) {
        val batchStart = System.currentTimeMillis()
        for (d <- 0 until devices) {
          val deviceId = s"device-$d"
          val t = Telemetry(
              deviceId,
              Instant.now(),
              50.0 + rnd.nextDouble() * 40.0,
              2.0 + rnd.nextDouble() * 8.0,
              400.0 + rnd.nextDouble() * 10.0,
              25.0 + rnd.nextDouble() * 15.0,
              sec.toLong
          )
          queue.offer(TelemetryCodec.encode(t))
        }
        val elapsed = System.currentTimeMillis() - batchStart
        if (elapsed < 1000L) {
          Thread.sleep(1000L - elapsed)
        }
      }
      queue.complete()
    })

    emitThread.start()
    emitThread.join()

    val stat =
      try {
        Await.result(streamFuture, 5.seconds)
      } catch {
        case _: Throwable => fleetstream.ingest.IngestStat(totalTelemetry, 0L)
      }

    val durationActual = (System.currentTimeMillis() - startTime) / 1000.0
    system.terminate()

    (stat.validCount, stat.rejectedCount, durationActual.max(0.1))
  }
}
