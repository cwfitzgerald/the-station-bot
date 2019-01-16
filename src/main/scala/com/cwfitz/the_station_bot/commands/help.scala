package com.cwfitz.the_station_bot.commands

import java.util.function.Consumer

import com.cwfitz.the_station_bot.{Client, Command}
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.{EmbedCreateSpec, MessageCreateSpec}

object help extends Command {
	private def helpMessage(spec: EmbedCreateSpec): Unit = spec
		.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
		.setTitle("Help")
		.setDescription("Help page for The Station's Bot.")
		.addField("!help <module>", "View help on a specific command. Modules: `help`, `speed`.", false)
		.addField("!ping", "View bot ping time.", false)
		.addField("!set/add <roles>", "add <roles> to your user separated by commas or spaces", false)
		.addField("!rem/remove <roles>", "remove <roles> from your user separated by commas or spaces", false)
		.addField("!speed", "Calculate speed of a train based on its length and the time it takes to cross a single point. `!help speed` for more.", false)
		.addField("!delays <count>", "See train delays. `!help delays` for more.", false)
		.addField("!L", "L", false)

	private def speedHelpMessage(spec: EmbedCreateSpec): Unit = spec
		.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
		.setTitle("!speed")
		.setDescription(
			"""Help for the speed command.
			  |There are 3 main clauses to a speed command. The `over`, `from`, and the `to` clauses.
			  |
			  |Example Commands:
			  |`!speed over x10 R160s from 1m10.63s to 1m28.32s`
			  |`!speed over 2 75fters from 2.63 seconds to 4.7 seconds`""".stripMargin
		)
		.addField("over", "The length of the train being measured. Comes in the form of a car count (`x10` or `10` or `10x`) and a car type (`R32` or `R32s`) or length (`75ft`, `75fters`).", false)
		.addField("from", "The time in the video where the train being measured crosses the measuring point. Accepts hours, minutes, and seconds (`1h1m10.63 seconds`).", false)
		.addField("to", "The time in the video where the last car of the train being measured crosses the measuring point. Accepts hours, minutes, and seconds  (`1 hour 32 minutes 28.32 seconds`).", false)

	private def lMessage(spec: EmbedCreateSpec): Unit = spec
		.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
		.setTitle("!L")
		.setDescription(
			"L"
		)

	private def delaysMessage(spec: EmbedCreateSpec): Unit = spec
		.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
		.setTitle("!delays <count>")
		.setDescription(
			"""Show the current delays. You can set the amount of delays by passing in a number.
			  |Defaults to a random number between 2 - 4
			  |If you are nice, you will choose 0 so we can get to work on time.
			  |
			  |Example Command:
			  |`!delays 6`""".stripMargin
		)
		.addField("count", "The amount of delays to show. Max 10.", false)

	def help(event: MessageCreateEvent, args: String): Unit = {
		val module = args.toLowerCase.split(' ').take(1)
		val embedFunc = module(0) match {
			case "l" => lMessage _
			case "speed" => speedHelpMessage _
			case "delays" => delaysMessage _
			case "help" => helpMessage _
			case "" => helpMessage _
			case m =>
				event.getMessage.getChannel.toScala.flatMap(
					chan => chan.createMessage(s"""Module "$m" doesn't have a help page.""").toScala
				).subscribe()
				return // TODO: This is bad and I should feel bad
		}

		event.getMessage.getChannel.toScala.flatMap {
			chan => chan.createMessage(x => x.setEmbed(embedFunc(_))).toScala
		}.subscribe()
	}

	override def apply(c: Client, event: MessageCreateEvent, args: String): Unit = {
		help(event, args)
	}
}
