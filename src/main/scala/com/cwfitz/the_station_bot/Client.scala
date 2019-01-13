package com.cwfitz.the_station_bot

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

import scala.collection.mutable

class Client (val client: IDiscordClient, val prefix: String) {
	private val commandMap: mutable.Map[String, Command] =
		mutable.HashMap[String, Command]()

	def addCommand(name: String, handler: Command): Client = {
		commandMap += name -> handler
		this
	}
	def removeCommand(name: String): Boolean = {
		commandMap.remove(name).isDefined
	}

	@EventSubscriber
	def messageReceived(e: MessageReceivedEvent): Unit = {
		val message = e.getMessage.getContent

		if (!e.getAuthor.isBot && message.startsWith(prefix)) {
			val command = message.drop(1).takeWhile(_ != ' ').toLowerCase
			val arg = message.dropWhile(_ != ' ').drop(1)
			commandMap.get(command) match {
				case Some(f) => f(this, e, arg)
				case None =>
					buffered {
						e.getChannel.sendMessage(s"Unknown command $command. `!help` to see all commands")
					}
			}
		}
	}
}
object Client {
	def apply(apiKey: String, prefix: String): Client = {
		val iClient = new ClientBuilder().withToken(apiKey).build()
		val client = new Client(iClient, prefix)
		iClient.getDispatcher.registerListeners(client)
		iClient.login()
		client
	}
}
