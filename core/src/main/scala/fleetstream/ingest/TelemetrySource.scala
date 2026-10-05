package fleetstream.ingest

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}

object TelemetrySource {
  def queue(
      bufferSize: Int,
      overflowStrategy: OverflowStrategy = OverflowStrategy.fail
  ): Source[Array[Byte], SourceQueueWithComplete[Array[Byte]]] = {
    Source.queue[Array[Byte]](bufferSize, overflowStrategy)
  }
}
