package com.cwfitz.the_station_bot

import akka.actor.Actor
import com.cwfitz.the_station_bot.D4JImplicits._
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConverters._

class CommandDispatch extends Actor {
	implicit val ec: ExecutionContext = context.system.dispatcher
	private val logger = LoggerFactory.getLogger(getClass)
	private val commandMap = mutable.HashMap[String, Command]()
	private val commandNames = mutable.ArrayBuffer[String]()

	override def receive: Receive = {
		case Client.AddCommand(name, action) =>
			logger.info(s"Adding command $name")
			commandMap += name -> action
			commandNames += name
		case Client.RemoveCommand(name) =>
			logger.info(s"Removing command $name")
			commandMap -= name
			commandNames -= name
		case Client.DispatchCommand(name, bundle) =>
			commandMap.get(name) match {
				case Some(f) => Future { f(bundle.client, bundle.event, bundle.command, bundle.args) }
				case None =>
					val fuzzyMatch = FuzzySearch.extractOne(name, commandNames.asJavaCollection)
					val issue = fuzzyMatch.getScore >= 75
					logger.info(s"""Incorrect command "$name". Fuzzymatched "${fuzzyMatch.getString}" at score ${fuzzyMatch.getScore}. ${if (issue) "Calling" else "Not Calling"}.""")
					if (issue) Future {
						bundle.event.getMessage.getChannel.toScala.flatMap {
							_.createMessage(s"You gave me the command `$name`, but that ain't a thing, so here's `${fuzzyMatch.getString}`.").toScala
						}.subscribe()
						val fuzzyFunction = commandMap(fuzzyMatch.getString)
						fuzzyFunction(bundle.client, bundle.event, fuzzyMatch.getString, bundle.args)
					}
					else {
						sender ! Client.EmptyCommand(name, bundle.event.getMessage.getChannel.toScala)
					}
			}
	}
}
