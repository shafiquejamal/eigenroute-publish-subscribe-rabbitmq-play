# Eigenroute Publish Subscribe

This project borrows heavily (almost entirely) from [JWT Scala](https://github.com/pauldijou/jwt-scala). 

## Installation

### SBT

```
resolvers += "Eigenroute maven repo" at "http://mavenrepo.eigenroute.com/"
libraryDependencies += "com.eigenroute" % "eigenroute-publish-subscribe" % "0.0.1"
```

## Use

```scala

class MyMessageSubscriber @Inject() (
    override val actorSystem: ActorSystem,
    override val lifecycle: ApplicationLifecycle
  )
  extends RabbitMQPublisherSubscriber[MyMessage] {

  override val exchange: String = "some-exchange"
  override val queueName: String = "some-queue"
  override val routingKey = "some-routing-key-like-MyMessage"
  override val routingActor: ActorRef = actorSystem.actorOf(RoutingActor.props, "MessageRouter")
  override val convert: (String) => Option[BrokerMessageType] = MessageBrokerMessageConverter.convert

}

class RoutingActor extends Actor with ActorLogging {

  override def receive = {

    case message : MyMessage =>
      println("MyMessage received:", message)
    case message  =>
      println("Some other message received", message)

  }

}

object RoutingActor {

  def props = Props(new RoutingActor())

}

```

Don't forget to bind the injected dependency in Module.scala.