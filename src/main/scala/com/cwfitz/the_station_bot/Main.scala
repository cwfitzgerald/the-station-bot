package com.cwfitz.the_station_bot

import akka.actor.{ActorSystem, Props}
import com.cwfitz.the_station_bot.database.DBWrapper

object Main {
	def main(args: Array[String]): Unit = {
		val apiKey = sys.env.get("API_KEY") match {
			case Some(v) => v
			case None =>
				println("Cannot find API_KEY.")
				sys.exit(1)
		}

		DBWrapper()

		val actorSystem = ActorSystem("the-station-bot")
		val client = Client(actorSystem, apiKey, "!")
		val pinger = actorSystem.actorOf(Props[commands.ping], "pinger")

		client ! ("", commands.empty)
		client ! Client.AddCommand("help", commands.help)
		client ! Client.AddCommand("add", commands.roles.add)
		client ! Client.AddCommand("set", commands.roles.add)
		client ! Client.AddCommand("rem", commands.roles.remove)
		client ! Client.AddCommand("remove", commands.roles.remove)
		client ! Client.AddCommand("delays", commands.delays)
		client ! Client.AddCommand("emoji", commands.emoji)
		client ! Client.AddCommand("trainspeak", commands.emoji)
		client ! Client.AddCommand("ll", commands.random)
		client ! Client.AddCommand("speed", commands.speed)
		client ! Client.AddCommandActor("ping", pinger)
		client ! Client.AddCommandActor("pong", pinger)

		client ! Client.Run
	}
}
