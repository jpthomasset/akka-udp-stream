import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import udpstream.UdpSource

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("MySystem")
    implicit val materializer = ActorMaterializer()

    val source = UdpSource(new InetSocketAddress("127.0.0.1", 9876), 100)

    source.runForeach(r => println("Received " + r.data.decodeString("UTF-8") + " from " + r.sender))
  }
}
