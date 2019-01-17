package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Command
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent

import scala.util.Random

object random extends Command {
	private val messages = Array(
			"The R143s has the Intel:registered: Inside",
			"I wanna cuck a R143 so badly",
			"I wanna _____ on a G train so badly",
			"Remember that time a homeless man pissed on the tracks of the G train\"s 3rd rail? I do!",
			"Remember that time some conductor on the N/W line kept on pissing out the window when the train was elavated? I do!",
			"Brian: Do you have any L train updates for us this morning?",
			"I\"m coming on now. Let\"s see what do I have to say.",
			"L: Good Service",
			"L: Delays",
			"L: Service Change",
			"L: Next Stop: Go to the <F> to pay respects.",
			"I built the Second Avenue Subway. Go to the <C> to pay respects.",
			"I own the MTA.",
			"I bult the MTA.",
			"I pissed on the MTA.",
			"I cucked the MTA.",
			"Byford's a russian :3",
			"Perfect",
			"relevant.",
			"The R143 piss scence",
			"The R143 takes a piss on the carnarise tunnel",
			"REPORT: The carnarsie tunnel remains structurally safe",
			"REPORT: The L just pissed on the carnarise tunnel",
			"SERVICE CHANGE: Take and at the same time, avoid the A/C/F/J/M/Z/G/7 trains",
			"SERVICE CHANGE: Follow @FakeMTA on Twitter for updates",
			"You can also use the MYmta app for the latest trip-planning information.",
			"I don't care",
			"No.",
			"Yes?",
			"Lol.",
			"No.",
			"LUL",
			"LMFAO",
			":O"
		)

	override def apply(c: ActorRef, event: MessageCreateEvent, command: String, args: String): Unit = {
		event.getMessage.getChannel.toScala.flatMap {
			chan => chan.createMessage(messages(Random.nextInt(messages.length))).toScala
		}.subscribe()
	}
}
