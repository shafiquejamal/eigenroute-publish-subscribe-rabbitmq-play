package com.eigenroute.plumbing

import play.api.libs.json._

trait PublisherSubscriber {

  def publish(message: JsValue, routingKey: String): Unit

}

