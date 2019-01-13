package com.cwfitz.the_station_bot.commands

import com.cwfitz.the_station_bot.{Client, Command, buffered}
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.EmbedBuilder

object help extends Command {
	private val helpMessage: EmbedObject =
		new EmbedBuilder()
			.appendField("!help", "view this message", false)
			.appendField("!set/add <roles>", "add <roles> to your user", false)
			.appendField("!rem/remove <roles>", "remove <roles> from your user", false)
			.appendField("!delays", "See train delays", false)
			.appendField("!L", "L", false)
			.build()

	override def apply(c: Client, event: MessageReceivedEvent, args: String): Unit = {
		buffered {
			event.getChannel.sendMessage(helpMessage)
		}
	}
}
