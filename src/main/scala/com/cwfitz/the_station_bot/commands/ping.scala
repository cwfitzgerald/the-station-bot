package com.cwfitz.the_station_bot.commands

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorRef}
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.MessageBundle
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory

import scala.collection.mutable

class ping extends Actor {
	private val logger = LoggerFactory.getLogger(getClass)
	var pingMap: mutable.HashMap[Snowflake, Instant] = mutable.HashMap[Snowflake, Instant]()

	def handle(c: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		val message = e.getMessage
		pingMap.get(message.getChannelId) match {
			case Some(timestamp) =>
				val time = timestamp.until(message.getTimestamp, ChronoUnit.MILLIS)
				val id = message.getChannelId
				message.edit(msg => msg.setContent(s"Pinged! ${time}ms")).block
				logger.info(s"Pinging ending on channel ${message.getChannelId.asLong} at ${message.getTimestamp.toString}")
				pingMap -= id
			case None =>
				val sendMessage = e.getMessage.getChannel.toScala.flatMap {
					chan => chan.createMessage("!pong").toScala
				}.subscribe()
				pingMap += message.getChannelId -> message.getTimestamp
				logger.info(s"Pinging starting on channel ${message.getChannelId.asLong} at ${message.getTimestamp.toString}")
		}
	}

	override def receive: Receive = {
		case MessageBundle(c, e, com, args) => handle(c, e, com, args)
	}
}
