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

		val client = Client(apiKey, "!")

		client.addCommand("", commands.empty)
		client.addCommand("help", commands.help)
		client.addCommand("add", commands.roles.add)
		client.addCommand("set", commands.roles.add)
		client.addCommand("rem", commands.roles.remove)
		client.addCommand("remove", commands.roles.remove)
		client.addCommand("delays", commands.delays)
		client.addCommand("l", commands.random)
		client.addCommand("speed", commands.speed)
		client.addCommand("ping", client.akkaForward(pinger))
		client.addCommand("pong", client.akkaForward(pinger))

		client.run()
	}
}
