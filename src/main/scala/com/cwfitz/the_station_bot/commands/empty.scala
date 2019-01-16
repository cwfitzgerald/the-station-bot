package com.cwfitz.the_station_bot.commands

import com.cwfitz.the_station_bot.{Client, Command}
import discord4j.core.event.domain.message.MessageCreateEvent

object empty extends Command{
	override def apply(c: Client, m: MessageCreateEvent, args: String): Unit = {}
}
