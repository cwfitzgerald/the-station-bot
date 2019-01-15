package com.cwfitz.the_station_bot.commands

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import com.cwfitz.the_station_bot.{Client, Command}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage

import scala.collection.immutable

object ping extends Command{
	private var pingMap = immutable.HashMap[IMessage, ZonedDateTime]()

	override def apply(c: Client, e: MessageReceivedEvent, args: String): Unit = {
		pingMap.get(e.getMessage) match {
			case Some(timestamp) =>
				val time = timestamp.until(ZonedDateTime.now(), ChronoUnit.MILLIS)
				e.getMessage.edit(s"Pinged! ${time}ms")
				pingMap -= e.getMessage
			case None =>
				val message = e.getChannel.sendMessage("!pong")
				pingMap += message -> ZonedDateTime.now()
		}
	}
}
