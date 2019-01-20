package com.cwfitz.the_station_bot

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.{GuildCreateEvent, GuildDeleteEvent}
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{DiscordClient, DiscordClientBuilder}
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.language.postfixOps

class Client (val client: DiscordClient) extends Actor {
	case class GuildInfo(prefix: String, defaultRole: Option[Long], adminRole: Long)
	private val guildInfos = mutable.LongMap[GuildInfo]()

	private val logger = LoggerFactory.getLogger(getClass)
	private val dispatcher = context.actorOf(Props[CommandDispatch], "dispatcher")

	private def akkaForward(actor: ActorRef) =
		(c: ActorRef, e: MessageCreateEvent, command: String, args: String) =>
			actor ! MessageBundle(c, e, command, args)

	private def guildCreated(e: GuildCreateEvent): Unit = {
		val id = e.getGuild.getId.asLong

		val exp = DBWrapper.guilds
			.filter(_.id === id)
			.map(g => (g.commandPrefix, g.defaultRole, g.adminRole))
			.result
		val v = Await.result(DBWrapper.database.run(exp), 1 second).headOption
		val data = v match {
			case Some((prefix, defaultRole, adminRole)) =>
				GuildInfo(prefix, if(defaultRole == 0) None else Some(defaultRole), adminRole)
			case None =>
				val setExp = DBWrapper.guilds += (id, "!", 0, id)
				DBWrapper.database.run(setExp)
				GuildInfo("!", None, id)
		}
		guildInfos += id -> data
	}

	private def guildRemoved(e: GuildDeleteEvent): Unit = {
		val id = e.getGuildId.asLong

		guildInfos.remove(id)
	}

	private def messageReceived(e: MessageCreateEvent): Unit = {
		val message = e.getMessage.getContent.orElse("")
		val guildID = e.getGuildId.get.asLong()
		val guildSettings = guildInfos(guildID)
		val prefix = guildSettings.prefix
		if (message.startsWith(prefix)) {
			val command = message.drop(prefix.length).takeWhile(_ != ' ').toLowerCase
			val arg = message.dropWhile(_ != ' ').drop(1)

			val member = e.getMember.get
			val guild = e.getGuild.toScala
			val adminRole = guild.flatMap(g => g.getRoleById(Snowflake.of(guildSettings.adminRole)).toScala)
			val everyoneRole = guild.flatMap(g => g.getRoleById(Snowflake.of(guildID)).toScala)

			member.getRoles.toScala.concatWith(everyoneRole)
				.flatMap(_.getPosition.toScala)
				.zipSingle(adminRole.flatMap(_.getPosition.toScala))
				.map{case(memberPos, adminPos) => memberPos >= adminPos}
				.reduce(_ || _)
    			.defaultIfEmpty(guildSettings.adminRole == guildID)
    			.flatMap(isAdmin => e.getMessage.getAuthor.toScala.map(
				    user => (isAdmin, user.getUsername)
			    ))
				.foreach{case (isAdmin, username) =>
					if (isAdmin) {
						dispatcher ! Client.DispatchCommandAdmin(command, MessageBundle(context.self, e, command, arg))
						logger.info(s"""Command sent by admin $username. Name "$command". Args: "$arg"""")
					}
					else {
						dispatcher ! Client.DispatchCommand(command, MessageBundle(context.self, e, command, arg))
						logger.info(s"""Command sent by user $username. Name "$command". Args: "$arg"""")
					}
				}
		}
	}

	def run(): Unit = {
		Future{ blocking { client.login().block() } }(ExecutionContext.global)
	}

	override def receive: Receive = {
		case Client.Run => run()
		case e: MessageCreateEvent => messageReceived(e)
		case e: GuildCreateEvent => guildCreated(e)
		case e: GuildDeleteEvent => guildRemoved(e)
		case ac: Client.AddCommand => dispatcher ! ac
		case adc: Client.AddAdminCommmand => dispatcher ! adc
		case Client.AddCommandActor(name, actor) => dispatcher ! Client.AddCommand(name, akkaForward(actor))
		case rc: Client.RemoveCommand => dispatcher ! rc
		case Client.UnknownCommand(name, channel) =>
			name.length match {
				case 0 =>
				case _ =>
					channel.flatMap (
						chan => chan.createMessage (s"Unknown command `$name`. Use `!help` to see all commands.").toScala
					).subscribe()
			}
		case Client.InsufficientPerms(name, channel) =>
			name.length match {
				case 0 =>
				case _ =>
					channel.flatMap (
						chan => chan.createMessage (s"Command `$name` is for admins only. You ain't that!").toScala
					).subscribe()
			}
		case Client.SendCommand(guild, channel, text) =>
			val prefix = guildInfos(guild.asLong).prefix
			channel.flatMap {
				chan => chan.createMessage(s"$prefix$text").toScala
			}.subscribe()
		case Client.SetGuildPrefix(id, prefix) =>
			val query = DBWrapper.guilds
				.filter(_.id === id.asLong)
				.map(_.commandPrefix)
				.update(prefix)
			DBWrapper.database.run(query)
			guildInfos.update(id.asLong, guildInfos(id.asLong).copy(prefix))
	}
}
object Client {
	case object Run
	sealed case class AddCommand(name: String, action: Command)
	sealed case class AddAdminCommmand(name: String, action: Command)
	sealed case class AddCommandActor(name: String, actor: ActorRef)
	sealed case class RemoveCommand(name: String)
	sealed case class DispatchCommand(name: String, bundle: MessageBundle)
	sealed case class DispatchCommandAdmin(name: String, bundle: MessageBundle)
	sealed case class UnknownCommand(name: String, channel: Mono[MessageChannel])
	sealed case class InsufficientPerms(name: String, channel: Mono[MessageChannel])
	sealed case class SendCommand(guildID: Snowflake, channel: Mono[MessageChannel], text: String)
	sealed case class SetGuildPrefix(id: Snowflake, prefix: String)

	def apply(actorSystem: ActorSystem, apiKey: String): ActorRef = {
		val iClient = new DiscordClientBuilder(apiKey).build()
		val client = actorSystem.actorOf(Props(classOf[Client], iClient))
		iClient.getEventDispatcher.on(classOf[MessageCreateEvent]).subscribe(client ! _)
		iClient.getEventDispatcher.on(classOf[GuildCreateEvent]).subscribe(client ! _)
		iClient.getEventDispatcher.on(classOf[GuildDeleteEvent]).subscribe(client ! _)
		client
	}
}
