package com.cwfitz.the_station_bot

import akka.actor.Actor

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class CommandDispatch extends Actor {
	implicit val ec: ExecutionContext = context.system.dispatcher
	private val commandMap = mutable.HashMap[String, Command]()

	override def receive: Receive = {
		case CommandDispatch.AddCommand(name, action) =>
			commandMap += name -> action
		case CommandDispatch.RemoveCommand(name) =>
			commandMap -= name
		case CommandDispatch.DispatchCommand(name, bundle) =>
			commandMap.get(name) match {
				case Some(f) => Future { f(bundle.c, bundle.e, bundle.args) }
				case None =>
					sender ! CommandDispatch.EmptyCommand(name)
			}
	}
}
object CommandDispatch {
	case class AddCommand(name: String, action: Command)
	case class RemoveCommand(name: String)
	case class DispatchCommand(name: String, bundle: MessageBundle)
	case class EmptyCommand(name: String)
}
