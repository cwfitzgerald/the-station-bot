package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.{ArgParser, Command}
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent

object inviteMe extends Command {
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: ArgParser.Argument): Unit = {
		e.getMessage.getChannel.toScala.flatMap {
			_.createMessage(
				"""To invite me to your server, use this link:
				  |<https://discordapp.com/api/oauth2/authorize?client_id=519701708851511306&permissions=469888000&scope=bot>""".stripMargin
			).toScala
		}.subscribe()
	}
}
