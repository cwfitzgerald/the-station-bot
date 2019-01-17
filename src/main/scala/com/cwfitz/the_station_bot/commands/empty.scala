package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Command
import discord4j.core.event.domain.message.MessageCreateEvent

object empty extends Command{
	override def apply(c: ActorRef, m: MessageCreateEvent, command: String, args: String): Unit = {}
}
