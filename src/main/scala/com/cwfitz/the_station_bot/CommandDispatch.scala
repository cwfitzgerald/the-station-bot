package com.cwfitz.the_station_bot

import akka.actor.Actor
import com.cwfitz.the_station_bot.Client.InsufficientPerms
import com.cwfitz.the_station_bot.D4JImplicits._
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext

class CommandDispatch extends Actor {
	implicit val ec: ExecutionContext = context.system.dispatcher
	private val logger = LoggerFactory.getLogger(getClass)
	private val commandMap = mutable.HashMap[String, Command]()
	private val adminMap = mutable.HashMap[String, Command]()
	private val commandNames = mutable.ArrayBuffer[String]()

	private def dispatchCommand(name: String, command: Option[Command], bundle: MessageBundle): Unit = {
		command match {
			case Some(f) =>
				ReportingFuture(logger) {
					f(bundle.client, bundle.event, bundle.command, bundle.args)
				}
			case None =>
				val (fuzzyMatch, fuzzyTime) = Time {
					FuzzySearch.extractOne(name, commandNames.asJavaCollection)
				}
				val issue = fuzzyMatch.getScore >= 75
				logger.info(f"""Incorrect command "$name". Fuzzymatched "${fuzzyMatch.getString}" at score ${fuzzyMatch.getScore}. ${if (issue) "Calling" else "Not Calling"}.""")
				logger.debug(f"""Fuzzy matching of "$name" -> "${fuzzyMatch.getString}" in ${fuzzyTime / 1000000.0}%.2fms.""")
				if (issue) ReportingFuture(logger) {
					bundle.event.getMessage.getChannel.toScala.flatMap {
						_.createMessage(s"You gave me the command `$name`, but that ain't a thing, so here's `${fuzzyMatch.getString}`.").toScala
					}.subscribe()
					val fuzzyFunction = commandMap(fuzzyMatch.getString)
					fuzzyFunction(bundle.client, bundle.event, fuzzyMatch.getString, bundle.args)
				}
				else {
					sender ! Client.UnknownCommand(name, bundle.event.getMessage.getChannel.toScala)
				}
		}
	}

	private def dispatchCommandUser(name: String, bundle: MessageBundle): Unit = {
		val command = commandMap.get(name)
		command match {
			case None => adminMap.get(name).foreach(_ => sender ! InsufficientPerms(name, bundle.event.getMessage.getChannel.toScala))
			case _ => dispatchCommand(name, commandMap.get(name), bundle)
		}

	}

	private def dispatchCommandAdmin(name: String, bundle: MessageBundle): Unit = {
		val command = adminMap.get(name).fold(commandMap.get(name))(Some(_))
		dispatchCommand(name, command, bundle)
	}

	override def receive: Receive = {
		case Client.AddCommand(name, action) =>
			logger.info(s"Adding command $name")
			commandMap += name -> action
			commandNames += name
		case Client.AddAdminCommand(name, action) =>
			logger.info(s"Adding admin command $name")
			adminMap += name -> action
		case Client.RemoveCommand(name) =>
			logger.info(s"Removing command $name")
			commandMap -= name
			commandNames -= name
		case Client.DispatchCommand(name, bundle) =>
			dispatchCommandUser(name, bundle)
		case Client.DispatchCommandAdmin(name, bundle) =>
			dispatchCommandAdmin(name, bundle)
	}
}
