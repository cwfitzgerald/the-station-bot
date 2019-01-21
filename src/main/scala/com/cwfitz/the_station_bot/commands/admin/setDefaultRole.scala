package com.cwfitz.the_station_bot.commands.admin

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Client.SetDefaultRole
import com.cwfitz.the_station_bot.Command
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent

object setDefaultRole extends Command {
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		val roleName = args.split(" ").head.toUpperCase
		val channel = e.getMessage.getChannel.toScala
		val guild = e.getGuild.toScala
		val guildID = e.getGuildId.get
		roleName.length match {
			case 0 =>
				client ! SetDefaultRole(guildID, None)
				channel.flatMap {
					c => c.createMessage(s"Removing default role.").toScala
				}.subscribe()
			case _ => guild
				.flatMapMany(_.getRoles.toScala)
				.collectSeq
				.foreach { roles =>
					val msg = roles.find(_.getName.toUpperCase == roleName) match {
						case Some(role) =>
							client ! SetDefaultRole(guildID, Some(role.getId))
							s"Setting role ${role.getMention} as default."
						case None =>
							s"Unknown role $roleName."
					}
					channel.flatMap {
						c => c.createMessage(msg).toScala
					}.subscribe()
				}
		}
	}
}
