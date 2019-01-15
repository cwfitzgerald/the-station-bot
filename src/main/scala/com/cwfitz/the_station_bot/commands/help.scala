package com.cwfitz.the_station_bot.commands

import com.cwfitz.the_station_bot.{Client, Command, buffered}
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.EmbedBuilder

object help extends Command {
	private val helpMessage: EmbedObject =
		new EmbedBuilder()
			.withAuthorName("Andrew Cuomo")
			.withAuthorIcon("https://cwfitz.com/s/_qaqGg.jpg")
			.withTitle("Help")
			.withDescription("Help page for The Station's Bot.")
			.appendField("!help <module>", "View help on a specific command. Modules: `help`, `speed`.", false)
			.appendField("!set/add <roles>", "add <roles> to your user separated by commas or spaces", false)
			.appendField("!rem/remove <roles>", "remove <roles> from your user separated by commas or spaces", false)
			.appendField("!speed", "Calculate speed of a train based on its length and the time it takes to cross a single point. `!help speed` for more.", false)
			.appendField("!delays <count>", "See train delays. `!help delays` for more.", false)
			.appendField("!L", "L", false)
			.build()

	private val speedHelpMessage: EmbedObject =
		new EmbedBuilder()
			.withAuthorName("Andrew Cuomo")
			.withAuthorIcon("https://cwfitz.com/s/_qaqGg.jpg")
			.withTitle("!speed")
			.withDescription(
				"""Help for the speed command.
				  |There are 3 main clauses to a speed command. The `over`, `from`, and the `to` clauses.
				  |
				  |Example Commands:
				  |`!speed over x10 R160s from 1m10.63s to 1m28.32s`
				  |`!speed over 2 75fters from 2.63 seconds to 4.7 seconds`""".stripMargin
			)
			.appendField("over", "The length of the train being measured. Comes in the form of a car count (`x10` or `10` or `10x`) and a car type (`R32` or `R32s`) or length (`75ft`, `75fters`).", false)
			.appendField("from", "The time in the video where the train being measured crosses the measuring point. Accepts hours, minutes, and seconds (`1h1m10.63 seconds`).", false)
			.appendField("to", "The time in the video where the last car of the train being measured crosses the measuring point. Accepts hours, minutes, and seconds  (`1 hour 32 minutes 28.32 seconds`).", false)
			.build()

	private val lMessage: EmbedObject =
		new EmbedBuilder()
			.withAuthorName("Andrew Cuomo")
			.withAuthorIcon("https://cwfitz.com/s/_qaqGg.jpg")
			.withTitle("!L")
			.withDescription(
				"L"
			)
			.build()

	private val delaysMessage: EmbedObject =
		new EmbedBuilder()
			.withAuthorName("Andrew Cuomo")
			.withAuthorIcon("https://cwfitz.com/s/_qaqGg.jpg")
			.withTitle("!delays <count>")
			.withDescription(
				"""Show the current delays. You can set the amount of delays by passing in a number.
				  |Defaults to a random number between 2 - 4
				  |If you are nice, you will choose 0 so we can get to work on time.
				  |
				  |Example Command:
				  |`!delays 6`""".stripMargin
			)
    	    .appendField("count", "The amount of delays to show. Max 10.", false)
			.build()

	override def apply(c: Client, event: MessageReceivedEvent, args: String): Unit = {
		val module = args.toLowerCase.split(' ').take(1)
		val message = module(0) match {
			case "l" => lMessage
			case "speed" => speedHelpMessage
			case "delays" => delaysMessage
			case "help" => helpMessage
			case "" => helpMessage
			case m =>
				buffered { event.getChannel.sendMessage(s"""Module "$m" doesn't have a help page.""") }
				return // TODO: This is bad and I should feel bad
		}
		buffered {
			event.getChannel.sendMessage(message)
		}
	}
}
