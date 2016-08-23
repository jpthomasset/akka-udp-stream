akka-udp-stream
===================

Sample code to create a akka source stream from UDP Datagram. The UdpSource will listen on a given InetSocketAddress and stream the received datagram.

Usage
------

Sample code to receive UDP datagram by listening on port 9876 on 127.0.0.1: 
```scala

implicit val system = ActorSystem("MySystem")
implicit val materializer = ActorMaterializer()

val source = UdpSource(new InetSocketAddress("127.0.0.1", 9876), 100)

source.runForeach(r => println("Received " + r.data.decodeString("UTF-8") + " from " + r.sender))

```

If you run the sample main, you can send data using netcat for example:
```
echo -n "Hello World" | nc -cu 127.0.0.1 9876
```
