package com.cwfitz.the_station_bot.commands.admin

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Client.SetAdminRole
import com.cwfitz.the_station_bot.{ArgParser, Command}
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent

object setAdminRole extends Command {
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, argPack: ArgParser.Argument): Unit = {
		val args = argPack.fullText
		val roleName = args.toUpperCase
		val channel = e.getMessage.getChannel.toScala
		val guild = e.getGuild.toScala
		val guildID = e.getGuildId.get
		roleName.length match {
			case 0 =>
				channel.flatMap {
					c => c.createMessage(s"Must provide a role.").toScala
				}.subscribe()
			case _ => guild
				.flatMapMany(_.getRoles.toScala)
				.collectSeq
				.foreach { roles =>
					val msg = roles.find(_.getName.toUpperCase == roleName) match {
						case Some(role) =>
							client ! SetAdminRole(guildID, role.getId)
							s"Setting role ${role.getMention} as admin."
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
