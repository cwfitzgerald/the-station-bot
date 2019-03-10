package com.cwfitz.the_station_bot.commands

import java.time.{ZoneId, ZonedDateTime}

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.{ArgParser, Command, EmojiFilter}
import discord4j.core.event.domain.message.MessageCreateEvent

import scala.util.{Random, Try}

object delays extends Command {
	private val delayMessages = Vector(
			"(1) trains are not stopping at 181 St due to reports of spooky noises coming from the elevators.",
			"(1) trains are running with delays after a southbound train got lost and ended up at the South Ferry Loop.",
			"The (2) and (5) sisters MAY feel like fucking up your afternoon by going express between 3rd ave and E 180st in The Bronx due to the never-ending track construction.",
			"(3) trains are running with delays after the doors of the rear five cars got opened at 145 St.",
			"(4), (5), and (6) trains are running with delays after an earlier incident involving a void between the train and the platform at 14 St-Union Square.",
			"Southbound (4) and (5) trains are delayed while the NYPD responds to someone who is refusing to stop dangling their legs over the platform at 14 St-Union Sq.",
			"(5) trains are running via the (2) after someone's shoe got stuck in the switch at 149 St-Grand Concourse.",
			"(5) trains are not running between Bowling Green and Flatbush Av after protests from (4) riders.",
			"The (2) and (3) OR (4) and (5) are delayed because someone requires medical assistance at Fulton St and/or Atlantic Ave.",
			"<7> trains will be running every 30 minutes, with three trains back-to-back after that due to earlier communication issues. By communication issues, we mean our intern spilled coffee on the computer again. Goddammit, Jason.",
			"<7> trains are not running as rush hour has been cancelled this <morn/eve>.",
			"(A) trains are running local north of Chambers as someone forgot that you can flip that switch.",
			"(B) trains aren't running because the MTA declared today a weekend.",
			"(B) trains are not running between Prospect Park and Brighton Beach, and (Q) trains are not running between Prospect Park and Coney Island-Stillwell Av due to goats on the tracks.",
			"Due to an earlier incident, southbound (B) and (D) trains are running local along 6 Av. Then again, they never were much of an express anyway.",
			"(C) trains are running with 10 cars due to accidental car swaps. It's beneficial, but do you _really_ think we would listen to customers?",
			"(D) trains are running via the (F) between West 4 St-Washington Sq and Coney Island-Stillwell Av, and (F) trains are running via the (D) between West 4 St-Washington Sq and Coney Island-Stillwell Av because rail control decided to make things a little interesting for this <morn/eve>'s commute.",
			"(E) trains are running local between Forest Hills-71 Av and Jamaica Center just to piss off Archer Avenue riders.",
			"(E) and (F) trains are enjoying a conga line into Forest Hills-71 Av and Jackson Heights-Roosevelt Av since passengers can't step aside to let other people off.",
			"(E), (F), (M), and (R) trains are running with delays because of Forest Hills-71 Av.",
			"(F) trains are currently hiding in the lower level of Bergen St.",
			"(G) trains will be stopping wherever they feel like along the platform for the forseeable future due to indecisive Train Operators. We apologize for the inconvenience, but at least you'll get some daily cardio!",
			"(L) trains are running every 20 minutes through only one of the tunnels all day every day until either Cuomo leaves office or the tunnel collapses.",
			"(L) trains are not running between Union Square and Bedford Avenue because we are nostalgic about the (L) train shutdown.",
			"Jamaica-bound (J) and (Z) trains are delayed while our train crew takes a restroom break at Crescent St. All trains are now Flushing-bound.",
			"(J) trains are not running due to Bombardier being Bombardier.",
			"(M) trains are running between Delancey St-Essex St and Metropolitan Av-Middle Village due to Chris Christie being stuck in the Chrystie Street Connection",
			"(M) trains didn't wake up today.",
			"(Q) trains aren't running because someone pissed in every single car.",
			"(Q) trains are running with delays after a stubborn N train refused to move past Herald Square.",
			"(N), (Q), (R), and (W) are running with delays in all 4 directions.",
			"(T) trains are hiding in the Twilight Zone due to signal problems. We expect them to be moving in a few decades.",
			"(Z) trains are not running today. But did they ever really run?"
		)

	override def apply(client: ActorRef, event: MessageCreateEvent, command: String, argPack: ArgParser.Argument): Unit = {
		val args = argPack.fullText
		val count = Try { args.toInt }.getOrElse(Random.nextInt(2) + 2).min(10)

		val msg = if (count == 0) {
			"Wow, there are no delays!"
		}
		else {
			val neg = count < 0
			val actualCount = if (neg) -count else count
			val hour = ZonedDateTime.now(ZoneId.of("America/New_York")).getHour
			val messages = Random.shuffle(delayMessages).take(actualCount).map(s => s" - $s").mkString("\n")
			val filtered = messages.replace("<morn/eve>", if (hour < 12) "morning" else "evening")
			val reversed = if (neg) filtered.replace(")", "(").replace("(", ")") else filtered
			val emoji = EmojiFilter.trainspeak(reversed)
			s"The current delays:\n$emoji"
		}
		event.getMessage.getChannel.toScala.flatMap {
			chan => chan.createMessage(msg).toScala
		}.subscribe()
	}
}
