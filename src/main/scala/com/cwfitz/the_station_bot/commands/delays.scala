package com.cwfitz.the_station_bot.commands

import com.cwfitz.the_station_bot.{Client, Command, buffered}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object delays extends Command {
	private val delayMessage: String =
		"""Delays:
		  | - The (2) and (5) sisters MAY feel like fucking up your afternoon by going express between 3rd ave and E 180st in The Bronx due to the never-ending track construction.
		  | - The (2) and (3) OR (4) and (5) are delayed because someone requires medical assistance at Fulton St and/or Atlantic Ave.
		  | - (L) trains are running every 20 minutes through only one of the tunnels all day every day until either Cuomo leaves office or the tunnel collapses.
		  | - (7) train would be running with delays in both directions if you see someone with a red shirt on the train on the middle cars.
		  | - Lastly, (N), (Q), (R), and (W) are running with delays in all 4 directions.
		  |""".stripMargin

	override def apply(c: Client, event: MessageReceivedEvent, args: String): Unit = {
		buffered {
			event.getChannel.sendMessage(delayMessage)
		}
	}
}
