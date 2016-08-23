package udpstream

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Udp.{Send, SimpleSender, SimpleSenderReady}
import akka.io.{IO, Udp}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, SocketUtil, TestKit, TestProbe}
import akka.util.ByteString
import scala.concurrent.duration._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class UdpSourceSpec extends TestKit(ActorSystem("UdpSource"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  implicit val materializer = ActorMaterializer()

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val simpleSender: ActorRef = {
    val commander = TestProbe()
    commander.send(IO(Udp),  SimpleSender)
    commander.expectMsg(SimpleSenderReady)
    commander.sender()
  }

  "An UdpSource" must {
    "stream received datagram" in {
      val serverAddress = SocketUtil.temporaryServerAddresses(1, udp = true).head
      val udpServer = UdpSource(serverAddress, 2)

      val sink = TestSink.probe[Udp.Received]
      val sub = udpServer.runWith(sink).request(1)

      val data = ByteString("Some data")

      simpleSender ! Send(data, serverAddress)
      sub.expectNext(500.millis).data should be (data)
      sub.cancel()
    }

    "discard datagram when buffer is full" in {
      val serverAddress = SocketUtil.temporaryServerAddresses(1, udp = true).head
      val udpServer = UdpSource(serverAddress, 100)
      val subscriber = TestSubscriber.manualProbe[Udp.Received]()

      udpServer.to(Sink.fromSubscriber(subscriber)).run()
      val subscription = subscriber.expectSubscription()

      // fill buffer
      for(i <- 1  to 200) simpleSender ! Send(ByteString(i), serverAddress)

      subscriber.expectNoMsg(500.millis)
      for(i <- 1 to 100) {
        subscription.request(1)
        subscriber.expectNext().data should be(ByteString(i))
      }

      subscription.request(1)
      subscriber.expectNoMsg(1.second)
      subscription.cancel()
    }

    "resume receiving when buffer is low again" in {
      val serverAddress = SocketUtil.temporaryServerAddresses(1, udp = true).head
      val udpServer = UdpSource(serverAddress, 100)
      val subscriber = TestSubscriber.manualProbe[Udp.Received]()

      udpServer.to(Sink.fromSubscriber(subscriber)).run()
      val subscription = subscriber.expectSubscription()

      // fill buffer
      for(i <- 1  to 200) simpleSender ! Send(ByteString(i), serverAddress)

      subscriber.expectNoMsg(500.millis)
      for(i <- 1 to 100) {
        subscription.request(1)
        subscriber.expectNext().data should be(ByteString(i))
      }

      for(i <- 201  to 300) simpleSender ! Send(ByteString(i), serverAddress)
      for(i <- 201 to 300) {
        subscription.request(1)
        subscriber.expectNext().data should be(ByteString(i))
      }

      subscription.cancel()
    }
  }

}