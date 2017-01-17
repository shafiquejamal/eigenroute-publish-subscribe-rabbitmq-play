package com.eigenroute.plumbing

import akka.actor._
import akka.routing.SmallestMailboxPool
import com.rabbitmq.client.{Channel, ConnectionFactory}
import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit.{Message, RabbitControl}
import com.thenewmotion.akka.rabbitmq._
import com.typesafe.config.ConfigFactory
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._

import scala.concurrent.Future

trait RabbitMQPublisherSubscriber extends PublisherSubscriber {

  val actorSystem: ActorSystem
  val lifecycle: ApplicationLifecycle
  val exchange: String
  val props: Props
  val nrOfInstances = 100
  val convert: (String) => Option[MessageBrokerMessageType]

  val rabbitControl = actorSystem.actorOf(Props[RabbitControl])

  override def publish(message: JsValue, routingKey: String): Unit = {
    rabbitControl ! Message.exchange(message, exchange, routingKey)
  }

  val conf = ConfigFactory.load()
  val queueName: String = conf.getString("eigenroute-publish-subscribe.queueName")

  val factory = new ConnectionFactory()
  factory.setHost(conf.getString("op-rabbit.connection.host"))
  factory.setPort(conf.getInt("op-rabbit.connection.port"))
  factory.setUsername(conf.getString("op-rabbit.connection.username"))
  factory.setPassword(conf.getString("op-rabbit.connection.password"))
  factory.setVirtualHost(conf.getString("op-rabbit.connection.virtual-host"))
  val useSSL = conf.getBoolean("op-rabbit.connection.ssl")
  if (useSSL) factory.useSslProtocol()

  import concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  val connectionActor: ActorRef = actorSystem.actorOf(ConnectionActor.props(factory, 3.seconds), "subscriber-connection")

  val incomingMessageHandler = actorSystem.actorOf(props.withRouter(SmallestMailboxPool(nrOfInstances = nrOfInstances)))


  def setupSubscriber(channel: Channel, self: ActorRef) {
    val queue = channel.queueDeclare(queueName, true, false, false, new java.util.HashMap()).getQueue
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) {
        val incomingMessageJson = Json.parse(new String(body))
        val maybeMessage = convert(envelope.getRoutingKey).flatMap(_.toMessageBrokerMessage(incomingMessageJson))
        maybeMessage.foreach { message => incomingMessageHandler ! message }
      }
    }
    channel.basicConsume(queue, true, consumer)
  }

  connectionActor ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))

  lifecycle.addStopHook { () =>
    Future(connectionActor ! PoisonPill)
  }

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

}
