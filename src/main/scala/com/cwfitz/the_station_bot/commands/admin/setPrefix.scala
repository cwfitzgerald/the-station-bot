package com.cwfitz.the_station_bot.commands.admin

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.{Client, Command}
import discord4j.core.event.domain.message.MessageCreateEvent


object setPrefix extends Command {
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		val guildID = e.getGuildId.get
		val channel = e.getMessage.getChannel.toScala
		val words = args.split(" ").filter(_.nonEmpty)
		val newPrefix = words.length match {
			case 0 =>
				channel.flatMap {
					c => c.createMessage("Reset prefix to `!`.").toScala
				}.subscribe()
				Some("!")
			case 1 =>
				val prefix = words.head
				channel.flatMap {
					c => c.createMessage(s"Set prefix to `$prefix`.").toScala
				}.subscribe()
				Some(prefix)
			case _ =>
				channel.flatMap {
					c => c.createMessage(s"Too many arguments.").toScala
				}.subscribe()
				None
		}
		newPrefix.foreach(prefix => client ! Client.SetGuildPrefix(guildID, prefix))
	}
}
