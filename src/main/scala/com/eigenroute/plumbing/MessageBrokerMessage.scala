package com.eigenroute.plumbing

import play.api.libs.json.JsValue

trait MessageBrokerMessage {

  def messageType: BrokerMessageType

}

trait BrokerMessageType {

  def toMessageBrokerMessage(msg: JsValue): Option[MessageBrokerMessage]

}

