package com.cwfitz.the_station_bot

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{DiscordClient, DiscordClientBuilder}
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.concurrent.{ExecutionContext, Future}

class Client (val client: DiscordClient, val prefix: String) extends Actor {
	private val logger = LoggerFactory.getLogger(getClass)
	private val dispatcher = context.actorOf(Props[CommandDispatch], "dispatcher")

	private def akkaForward(actor: ActorRef) =
		(c: ActorRef, e: MessageCreateEvent, command: String, args: String) =>
			actor ! MessageBundle(c, e, command, args)

	def messageReceived(e: MessageCreateEvent): Unit = {
		val message = e.getMessage.getContent.orElse("")

		if (message.startsWith(prefix)) {
			val command = message.drop(1).takeWhile(_ != ' ').toLowerCase
			val arg = message.dropWhile(_ != ' ').drop(1)

			for {
				author <- e.getMessage.getAuthor.toScala
			} logger.info(s"""Command sent by ${author.getUsername}. Name "$command". Args: "$arg"""")

			dispatcher ! Client.DispatchCommand(command, MessageBundle(context.self, e, command, arg))
		}
	}

	def run(): Unit = {
		Future{ client.login().block() }(ExecutionContext.global)
	}

	override def receive: Receive = {
		case Client.Run => run()
		case Client.MessageReceived(e) => messageReceived(e)
		case ac: Client.AddCommand => dispatcher ! ac
		case Client.AddCommandActor(name, actor) => dispatcher ! Client.AddCommand(name, akkaForward(actor))
		case rc: Client.RemoveCommand => dispatcher ! rc
		case Client.EmptyCommand(name, channel) =>
			channel.flatMap(
				chan => chan.createMessage(s"Unknown command `$name`. Use `!help` to see all commands.").toScala
			).subscribe()
	}
}
object Client {
	case object Run
	sealed case class MessageReceived(e: MessageCreateEvent)
	sealed case class AddCommand(name: String, action: Command)
	sealed case class AddCommandActor(name: String, actor: ActorRef)
	sealed case class RemoveCommand(name: String)
	sealed case class DispatchCommand(name: String, bundle: MessageBundle)
	sealed case class EmptyCommand(name: String, channel: Mono[MessageChannel])

	def apply(actorSystem: ActorSystem, apiKey: String, prefix: String): ActorRef = {
		val iClient = new DiscordClientBuilder(apiKey).build()
		val client = actorSystem.actorOf(Props(classOf[Client], iClient, prefix))
		iClient.getEventDispatcher.on(classOf[MessageCreateEvent]).subscribe(client ! MessageReceived(_))
		client
	}
}
