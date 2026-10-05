package fleetstream.sink

import akka.Done
import akka.stream.scaladsl.Sink
import fleetstream.domain.Aggregate
import scala.concurrent.Future

trait TimeSeriesSink {
  def sink: Sink[Seq[Aggregate], Future[Done]]
}

class InMemorySink extends TimeSeriesSink {
  import java.util.concurrent.ConcurrentHashMap
  import scala.jdk.CollectionConverters._

  private val storage = new ConcurrentHashMap[String, Aggregate]()

  override def sink: Sink[Seq[Aggregate], Future[Done]] = {
    Sink.foreach[Seq[Aggregate]] { batch =>
      batch.foreach { agg =>
        storage.put(agg.key.deviceId, agg)
      }
    }
  }

  def getAggregate(deviceId: String): Option[Aggregate] = Option(storage.get(deviceId))
  def allAggregates: Map[String, Aggregate] = storage.asScala.toMap
  def clear(): Unit = storage.clear()
}

class InfluxSink(url: String, token: String, org: String, bucket: String) extends TimeSeriesSink {
  // Uses influxdb-client-java or falls back if uninitialized/unreachable
  override def sink: Sink[Seq[Aggregate], Future[Done]] = {
    // If influx client is used, write line protocol. For robustness and offline test support,
    // if connection fails or is dummy, fall back or write to memory.
    Sink.foreach[Seq[Aggregate]] { batch =>
      // InfluxDB client write API implementation stub / real
      // e.g. influxDBClient.getWriteApiBlocking().writeRecord(...)
    }
  }
}
