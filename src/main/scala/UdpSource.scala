package udpstream

import java.net.InetSocketAddress

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source

import scala.annotation.tailrec

object UdpSource {
  def props(listenOn: InetSocketAddress, maxBufferSize: Int = 100): Props = Props(new UdpSource(listenOn, maxBufferSize))
  def apply(listenOn: InetSocketAddress, maxBufferSize: Int = 100) = Source.actorPublisher[Udp.Received](UdpSource.props(listenOn, maxBufferSize))
}

/**
  * UDP Datagram Source stream
  * @param listenOn the address & port to listen on
  * @param maxBufferSize the maximum buffer size before dropping datagrams
  */
class UdpSource(listenOn: InetSocketAddress, maxBufferSize: Int) extends ActorPublisher[Udp.Received] with ActorLogging {
  import context.system
  import akka.stream.actor.ActorPublisherMessage._

  require(maxBufferSize > 0, "UdpSource bufferSize must be greater than 0 to receive datagram")

  var datagrams= Vector.empty[Udp.Received]

  IO(Udp) ! Udp.Bind(self, listenOn)

  def receive = {
    case Udp.Bound(local) =>
      log.info(s"UdpSource ready to receive from $local")
      context.become(ready(sender()) orElse commonReceive(sender()))

    case Cancel =>
      context.stop(self)
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(_, _) if datagrams.size == maxBufferSize =>
      log.warning("Datagram buffer size ({}) exceeded", maxBufferSize)
      context.become(suspended(socket))

    case r:Udp.Received =>
      log.debug("Received datagram")
      if (datagrams.isEmpty && totalDemand > 0)
        onNext(r)
      else {
        log.debug("Buffering datagram")
        datagrams :+= r
        deliver()
      }

    case Request(_) => deliver()
  }

  def suspended(socket: ActorRef): Receive = {
    case Udp.Received =>
      log.debug("Dropping UDP datagram while suspended")

    case Request(_) =>
      deliver()
      log.info("Datagram buffer size is ok, resuming")
      context.become(ready(socket) orElse commonReceive(socket))
  }

  def commonReceive(socket: ActorRef): Receive = {
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => onCompleteThenStop()
    case Cancel => socket ! Udp.Unbind
  }

  @tailrec final def deliver(): Unit = {
    log.debug("ask for deliver")
    if(totalDemand > 0 && !datagrams.isEmpty) {
      val (use, keep) = datagrams.splitAt(math.min(totalDemand, Int.MaxValue).toInt)
      datagrams = keep
      use foreach onNext
      deliver()
    }
  }


}
