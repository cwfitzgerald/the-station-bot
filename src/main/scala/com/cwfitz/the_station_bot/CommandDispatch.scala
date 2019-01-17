package com.cwfitz.the_station_bot

import akka.actor.Actor
import com.cwfitz.the_station_bot.D4JImplicits._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class CommandDispatch extends Actor {
	implicit val ec: ExecutionContext = context.system.dispatcher
	private val commandMap = mutable.HashMap[String, Command]()

	override def receive: Receive = {
		case Client.AddCommand(name, action) =>
			commandMap += name -> action
		case Client.RemoveCommand(name) =>
			commandMap -= name
		case Client.DispatchCommand(name, bundle) =>
			commandMap.get(name) match {
				case Some(f) => Future { f(bundle.client, bundle.event, bundle.command, bundle.args) }
				case None =>
					sender ! Client.EmptyCommand(name, bundle.event.getMessage.getChannel.toScala)
			}
	}
}
