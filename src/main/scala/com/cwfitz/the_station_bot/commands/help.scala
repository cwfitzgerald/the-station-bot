package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.{ArgParser, Command, EmojiFilter, Time}
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import org.slf4j.LoggerFactory

object help extends Command {
	val logger = LoggerFactory.getLogger(getClass)

	private def helpMessage(spec: EmbedCreateSpec): Unit = spec
		.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
		.setTitle("Help")
		.setDescription("Help page for The Station's Bot.")
		.addField("!help <module>", "View help on a specific command. Modules: `help`, `speed`, `emoji`, `trainspeak`.", false)
		.addField("!ping", "View bot ping time.", false)
		.addField("!status", "View bot status.", false)
		.addField("!set/add <roles>", "add <roles> to your user separated by commas or spaces", false)
		.addField("!rem/remove <roles>", "remove <roles> from your user separated by commas or spaces", false)
		.addField("!speed", "Calculate speed of a train based on its length and the time it takes to cross a single point. `!help speed` for more.", false)
		.addField("!delays <count>", "See train delays. `!help delays` for more.", false)
		.addField("!emoji <msg>", "Perform various text replacements so you speak in emoji. `!help emoji` for more.", false)
		.addField("!trainspeak <msg>", "Perform various text replacements on the input string including adding line bullet emoji. `!help trainspeak` for more.", false)
		.addField("!ll", "ll", false)

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

	private def emojiSpeak(spec: EmbedCreateSpec): Unit = {
		spec.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
			.setTitle("!emoji <msg>")
			.setDescription(
				"""Filters the input message through an emoji filter.
				  |
				  |Text will first be run through the `trainspeak` filter.
				  |
				  |The following replacements will be made:
				  |""".stripMargin
			)
		for (emojiGroup <- EmojiFilter.emojiMap.toSeq.grouped((EmojiFilter.emojiMap.size + 2) / 3)) {
			spec.addField(s"​", emojiGroup.map(emoji => s"""`${emoji._1}` -> ${emoji._2}\n""").mkString, true)
		}
		spec.addField("\u200B", "`Everything else` -> 🅱", true)
	}

	private def trainSpeakMessage(spec: EmbedCreateSpec): Unit = {
		spec.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
			.setTitle("!trainspeak <msg>")
			.setDescription(
				"""Filters the input message through a trainspeak filter.
				  |
				  |"(7)" -> <:7_Train:333805562414366720>
				  |
				  |The following replacements will be made:
				  |""".stripMargin
			)
		for (emojiGroup <- EmojiFilter.trainSpeakMap.toSeq.grouped((EmojiFilter.trainSpeakMap.size + 2) / 3)) {
			spec.addField("\u200B​", emojiGroup.map(emoji => s"""`${emoji._1}` -> ${emoji._2}\n""").mkString, true)
		}
	}

	def help(event: MessageCreateEvent, args: String): Unit = {
		val module = args.toLowerCase.split(' ').take(1)
		val embedFunc = module(0) match {
			case "l" => lMessage _
			case "speed" => speedHelpMessage _
			case "delays" => delaysMessage _
			case "emoji" => emojiSpeak _
			case "trainspeak" => trainSpeakMessage _
			case "help" => helpMessage _
			case "" => helpMessage _
			case m =>
				event.getMessage.getChannel.toScala.flatMap(
					chan => chan.createMessage(s"""Module "$m" doesn't have a help page.""").toScala
				).subscribe()
				return // TODO: This is bad and I should feel bad
		}

		event.getMessage.getChannel.toScala.flatMap {
			chan => chan.createMessage(x => x.setEmbed(x => { val (_, t) = Time { embedFunc(x) }; logger.debug(f"Printed help in ${t / 1000000.0}%.2fms") })).toScala
		}.subscribe()
	}

	override def apply(c: ActorRef, event: MessageCreateEvent, command: String, args: ArgParser.Argument): Unit = {
		help(event, args.fullText)
	}
}
