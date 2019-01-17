package com.cwfitz.the_station_bot

import akka.actor.{ActorSystem, Props}

object Main {
	def main(args: Array[String]): Unit = {
		val apiKey = sys.env.get("API_KEY") match {
			case Some(v) => v
			case None =>
				println("Cannot find API_KEY.")
				sys.exit(1)
		}

		val actorSystem = ActorSystem("the-station-bot")
		val pinger = actorSystem.actorOf(Props[commands.ping], "pinger")

		val client = Client(actorSystem, apiKey, "!")

		client.addCommand("", commands.empty)
		client.addCommand("help", commands.help)
		client.addCommand("add", commands.roles.add _)
		client.addCommand("set", commands.roles.add _)
		client.addCommand("rem", commands.roles.remove _)
		client.addCommand("remove", commands.roles.remove _)
		client.addCommand("delays", commands.delays)
		client.addCommand("l", commands.random)
		client.addCommand("speed", commands.speed)
		client.addCommand("ping", pinger)
		client.addCommand("pong", pinger)

		client.run()
	}
}
