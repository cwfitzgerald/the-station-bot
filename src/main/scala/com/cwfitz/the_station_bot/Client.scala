package com.cwfitz.the_station_bot

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{DiscordClient, DiscordClientBuilder}
import org.slf4j.LoggerFactory

import scala.collection.mutable

class Client (val client: DiscordClient, val prefix: String) {
	private val logger = LoggerFactory.getLogger(getClass)

	private val commandMap: mutable.Map[String, Command] =
		mutable.HashMap[String, Command]()

	def akkaForward(actor: ActorRef): (Client, MessageCreateEvent, String) => Unit =
		(c: Client, e: MessageCreateEvent, args: String) =>
			actor ! MessageBundle(c, e, args)

	def addCommand(name: String, handler: Command): Client = {
		commandMap += name -> handler
		this
	}
	def removeCommand(name: String): Boolean = {
		commandMap.remove(name).isDefined
	}

	def messageReceived(e: MessageCreateEvent): Unit = {
		val message = e.getMessage.getContent.orElse("")

		if (message.startsWith(prefix)) {
			val command = message.drop(1).takeWhile(_ != ' ').toLowerCase
			val arg = message.dropWhile(_ != ' ').drop(1)

			for {
				author <- e.getMessage.getAuthor.toScala
			} logger.info(s"""Command sent by ${author.getUsername}. Name "$command". Args: "$arg"""")

			commandMap.get(command) match {
				case Some(f) => f(this, e, arg)
				case None =>
					println(command)
					e.getMessage.getChannel.toScala.flatMap(
						chan => chan.createMessage(s"Unknown command $command. `!help` to see all commands").toScala
					).subscribe()
			}
		}
	}

	def run(): Unit = {
		client.login().block()
	}
}
object Client {
	def apply(apiKey: String, prefix: String): Client = {
		val iClient = new DiscordClientBuilder(apiKey).build()
		val client = new Client(iClient, prefix)
		//noinspection ConvertibleToMethodValue
		iClient.getEventDispatcher.on(classOf[MessageCreateEvent]).subscribe(client.messageReceived(_))
		client
	}
}
