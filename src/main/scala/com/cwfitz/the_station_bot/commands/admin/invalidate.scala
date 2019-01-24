package com.cwfitz.the_station_bot.commands.admin

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Client.Invalidate
import com.cwfitz.the_station_bot.{ArgParser, Command}
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent

object invalidate extends Command{
	override def apply(client: ActorRef, e: MessageCreateEvent, name: String, args: ArgParser.Argument): Unit = {
		client ! Invalidate(e.getMessage.getChannel.toScala)
	}
}
