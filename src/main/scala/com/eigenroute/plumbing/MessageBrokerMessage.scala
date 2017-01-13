package com.eigenroute.plumbing

import play.api.libs.json.JsValue

trait MessageBrokerMessage {

  def messageType: MessageBrokerMessageType

}

trait MessageBrokerMessageType {

  def toMessageBrokerMessage(msg: JsValue): Option[MessageBrokerMessage]

}

