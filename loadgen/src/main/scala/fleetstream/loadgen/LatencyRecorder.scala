package fleetstream.loadgen

import org.HdrHistogram.Histogram

class LatencyRecorder {
  private val ingestHistogram =
    new Histogram(1, 6000000000L, 3) // up to 100 minutes in microseconds

  def recordIngestLatency(latencyMicros: Long): Unit = {
    if (latencyMicros > 0) {
      ingestHistogram.recordValue(latencyMicros.min(6000000000L))
    }
  }

  def printSummary(throughput: Double, validCount: Long, rejectedCount: Long): Unit = {
    println(f"=== LoadGen Latency & Throughput Summary ===")
    println(
        f"Total Valid: $validCount%d | Rejected: $rejectedCount%d | Throughput: $throughput%.2f msg/s"
    )
    println(f"Ingest Latency (p50): ${ingestHistogram.getValueAtPercentile(50.0) / 1000.0}%.2f ms")
    println(f"Ingest Latency (p99): ${ingestHistogram.getValueAtPercentile(99.0) / 1000.0}%.2f ms")
    println(
        f"Ingest Latency (p99.9): ${ingestHistogram.getValueAtPercentile(99.9) / 1000.0}%.2f ms"
    )
    println(f"Ingest Latency (max): ${ingestHistogram.getMaxValue / 1000.0}%.2f ms")
  }
}
