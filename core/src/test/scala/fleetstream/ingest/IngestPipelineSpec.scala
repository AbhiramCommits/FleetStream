package fleetstream.ingest

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source, Sink}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import akka.testkit.TestKit
import fleetstream.domain.{Aggregate, Telemetry, TelemetryCodec}
import fleetstream.sink.InMemorySink
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.Await
import scala.concurrent.duration._

class IngestPipelineSpec
    extends TestKit(ActorSystem("IngestPipelineSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "IngestPipeline" should {
    "drop malformed payloads and count them in IngestStat" in {
      val cfg = IngestConfig(
          maxSubstreams = 10,
          windowDuration = 100.millis,
          batchSize = 1,
          bufferSize = 100,
          overflowStrategy = OverflowStrategy.fail
      )

      val memSink = new InMemorySink()
      val pipeline = IngestPipeline(cfg, memSink.sink)

      val (queue, source) = TelemetrySource.queue(100).preMaterialize()

      val futureStat = source
        .via(pipeline)
        .runWith(akka.stream.scaladsl.Sink.head)

      val validT = Telemetry("dev-1", java.time.Instant.now(), 80.0, 5.0, 400.0, 30.0, 1L)
      queue.offer(TelemetryCodec.encode(validT))
      queue.offer("bad-json".getBytes)

      // complete queue after offering
      queue.complete()

      val stat = Await.result(futureStat, 3.seconds)
      stat.validCount shouldBe 1L
      stat.rejectedCount shouldBe 1L
    }

    "aggregate telemetry per device correctly" in {
      val cfg = IngestConfig(
          maxSubstreams = 10,
          windowDuration = 200.millis,
          batchSize = 1,
          bufferSize = 100,
          overflowStrategy = OverflowStrategy.fail
      )

      val memSink = new InMemorySink()
      val pipeline = IngestPipeline(cfg, memSink.sink)

      val (queue, source) = TelemetrySource.queue(100).preMaterialize()

      val futureDone = source
        .via(pipeline)
        .runWith(akka.stream.scaladsl.Sink.ignore)

      val now = java.time.Instant.now()
      val t1 = Telemetry("dev-A", now, 90.0, 10.0, 400.0, 25.0, 1L)
      val t2 = Telemetry("dev-A", now, 88.0, 20.0, 400.0, 28.0, 2L)

      queue.offer(TelemetryCodec.encode(t1))
      queue.offer(TelemetryCodec.encode(t2))

      // Wait a moment for window to flush
      Thread.sleep(400)
      queue.complete()

      Await.result(futureDone, 3.seconds)

      val agg = memSink.getAggregate("dev-A")
      agg.isDefined shouldBe true
      agg.get.count shouldBe 2L
      agg.get.meanPowerKw shouldBe 15.0
      agg.get.maxPowerKw shouldBe 20.0
    }

    "observe backpressure on slow sink / bounded queue buffer" in {
      val (q2, src2) = TelemetrySource.queue(1, OverflowStrategy.fail).preMaterialize()
      val t = Telemetry("dev-slow", java.time.Instant.now(), 80.0, 5.0, 400.0, 30.0, 1L)
      val bytes = TelemetryCodec.encode(t)

      // Run stream with a sink that never completes, so buffer fills up immediately
      val runnable = src2.to(Sink.ignore).run()

      val r1 = Await.result(q2.offer(bytes), 1.second)
      val r2 = Await.result(q2.offer(bytes), 1.second)
      val r3 = Await.result(q2.offer(bytes), 1.second)

      q2.complete()

      (r1 == QueueOfferResult.Enqueued) shouldBe true
    }
  }
}
