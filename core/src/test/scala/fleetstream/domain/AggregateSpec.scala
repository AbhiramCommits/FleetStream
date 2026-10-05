package fleetstream.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.time.Instant

class AggregateSpec extends AnyWordSpec with Matchers {

  "TelemetryCodec" should {
    "round-trip telemetry successfully" in {
      val t = Telemetry("dev-1", Instant.ofEpochMilli(1700000000000L), 85.5, 5.2, 400.0, 32.5, 42L)
      val encoded = TelemetryCodec.encode(t)
      val decoded = TelemetryCodec.decode(encoded)
      decoded shouldBe Right(t)
    }

    "return Left on malformed json" in {
      val badBytes = "{invalid-json}".getBytes
      TelemetryCodec.decode(badBytes).isLeft shouldBe true
    }
  }

  "Aggregate algebra" should {
    "be associative for merge" in {
      val t1 = Telemetry("dev-1", Instant.ofEpochMilli(1000L), 80.0, 2.0, 400.0, 30.0, 1L)
      val t2 = Telemetry("dev-1", Instant.ofEpochMilli(1000L), 79.0, 4.0, 400.0, 31.0, 2L)
      val t3 = Telemetry("dev-1", Instant.ofEpochMilli(1000L), 78.0, 6.0, 400.0, 32.0, 3L)

      val a1 = Aggregate.from(Seq(t1))
      val a2 = Aggregate.from(Seq(t2))
      val a3 = Aggregate.from(Seq(t3))

      val m1 = Aggregate.merge(Aggregate.merge(a1, a2), a3)
      val m2 = Aggregate.merge(a1, Aggregate.merge(a2, a3))

      m1.count shouldBe m2.count
      m1.meanPowerKw shouldBe m2.meanPowerKw
      m1.maxPowerKw shouldBe m2.maxPowerKw
      m1.minSocPercent shouldBe m2.minSocPercent
      m1.maxTempC shouldBe m2.maxTempC
    }

    "equal folding merge over single-element aggregates" in {
      val ts = Seq(
        Telemetry("dev-1", Instant.ofEpochMilli(1000L), 80.0, 2.0, 400.0, 30.0, 1L),
        Telemetry("dev-1", Instant.ofEpochMilli(1000L), 79.0, 4.0, 400.0, 31.0, 2L),
        Telemetry("dev-1", Instant.ofEpochMilli(1000L), 78.0, 6.0, 400.0, 32.0, 3L)
      )

      val direct = Aggregate.from(ts)
      val folded = ts.map(t => Aggregate.from(Seq(t))).reduce(Aggregate.merge)

      direct.count shouldBe folded.count
      direct.meanPowerKw shouldBe folded.meanPowerKw
      direct.maxPowerKw shouldBe folded.maxPowerKw
      direct.minSocPercent shouldBe folded.minSocPercent
      direct.maxTempC shouldBe folded.maxTempC
    }
  }
}
