package com.cwfitz.the_station_bot

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.{GuildCreateEvent, GuildDeleteEvent, MemberJoinEvent}
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{DiscordClient, DiscordClientBuilder}
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, blocking}
import scala.language.postfixOps

class Client (val client: DiscordClient) extends Actor {
	implicit val ec: ExecutionContext = context.dispatcher

	case class GuildInfo(prefix: String, defaultRole: Option[Long], adminRole: Long)
	private val guildInfos = mutable.LongMap[GuildInfo]()

	private val logger = LoggerFactory.getLogger(getClass)
	private val dispatcher = context.actorOf(Props[CommandDispatch], "dispatcher")

	private def akkaForward(actor: ActorRef) =
		(c: ActorRef, e: MessageCreateEvent, command: String, args: ArgParser.Argument) =>
			actor ! MessageBundle(c, e, command, args)

	private def SQLGuildLookupFunc(id: Rep[Long]) = {
		DBWrapper.guilds
			.filter(_.id === id)
			.map(g => (g.commandPrefix, g.defaultRole, g.adminRole))
	}
	private val SQLGuildLookup = Compiled(SQLGuildLookupFunc _)

	private def guildCreated(id: Long): Unit = {
		val v = Await.result(DBWrapper.database.run(SQLGuildLookup(id).result), 1 second).headOption
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

	private def SQLGuildListFunc() = {
		DBWrapper.guilds
			.map(g => (g.id, g.commandPrefix, g.defaultRole, g.adminRole))
	}
	private val SQLGuildList = SQLGuildListFunc().result

	private def refreshGuilds(replyChannel: Mono[MessageChannel]): Unit = {
		val (guildCount, time) = Time {
			val v = Await.result(DBWrapper.database.run(SQLGuildList), 1 second)
			v.foreach { case (id, prefix, defaultRole, adminRole) =>
				val data = GuildInfo(prefix, if (defaultRole == 0) None else Some(defaultRole), adminRole)
				guildInfos += id -> data
			}
			v.size
		}
		replyChannel.flatMap {
			c => c.createMessage(f"Refreshed $guildCount guilds in ${time / 1000000.0}%.2fms.").toScala
		}.subscribe()
	}

	private def guildRemoved(id: Long): Unit = {
		guildInfos.remove(id)
	}

	def memberAdded(e: MemberJoinEvent): Unit = {
		val member = e.getMember
		val defaultRole = guildInfos(e.getGuildId.asLong()).defaultRole
		defaultRole match {
			case Some(role) =>
				logger.info(s"Member ${member.getNicknameMention} joined. Adding role $role.")
				member.addRole(Snowflake.of(role)).subscribe()
			case None =>
				logger.info(s"Member ${member.getNicknameMention} joined. Default role disabled.")
		}
	}

	private def messageReceived(e: MessageCreateEvent): Unit = {
		if (e.getGuildId.toScala.isEmpty) {
			if (e.getMessage.getAuthorId != client.getSelfId) {
				e.getMessage.getChannel.toScala.flatMap {
					_.createMessage("Fuckoff m8. Luv U").toScala
				}.subscribe()
			}
			return
		}
		logger.debug("Message received")
		val message = e.getMessage.getContent.orElse("")
		val guildID = e.getGuildId.get.asLong()
		val guildSettings = guildInfos(guildID)
		val prefix = guildSettings.prefix
		if (message.startsWith(prefix)) ReportingFuture(logger) {
			logger.debug("Processing as command")

			val command = message.drop(prefix.length).takeWhile(_ != ' ').toLowerCase
			val arg = ArgParser(message.dropWhile(_ != ' '))

			val member = e.getMember.get
			val guild = e.getGuild.toScala
			val adminRole = guild.flatMap{_.getRoleById(Snowflake.of(guildSettings.adminRole)).toScala}
			val adminRolePos = adminRole.flatMap{ _.getPosition.toScala }
			val everyoneRole = guild.flatMap{_.getRoleById(Snowflake.of(guildID)).toScala}

			member.getRoles.toScala
				.startWith(everyoneRole)
				.takeLast(1)
				.map { r => logger.debug(s"Got role ${r.getName}"); r }
				.flatMap(_.getPosition.toScala)
				.zipWith(adminRolePos)
				.map { case (memberPos, adminPos) => memberPos >= adminPos }
				.reduce(_ || _)
				.defaultIfEmpty(guildSettings.adminRole == guildID)
				.flatMap { isAdmin =>
					logger.debug("Getting author.");
					e.getMessage.getAuthor.toScala.map {
						user => logger.debug("Got author."); (isAdmin, user.getUsername)
					}
				}
				.foreach { case (isAdmin, username) =>
					logger.debug("Chain finished.")
					if (isAdmin) {
						dispatcher ! Client.DispatchCommandAdmin(command, MessageBundle(context.self, e, command, arg))
					}
					else {
						dispatcher ! Client.DispatchCommand(command, MessageBundle(context.self, e, command, arg))
					}
					val adminText = if(isAdmin) "admin" else "user"
					logger.info(s"""Command sent by $adminText $username. Name "$command". Arg text: "${arg.fullText}". Split into ${arg.argc}: "${arg.argv.mkString("\", \"")}".""")
				}

		}
		logger.debug("Message receive event processed.")
	}

	def run(): Unit = {
		ReportingFuture(logger) { blocking { client.login().block() } }(ExecutionContext.global)
	}

	private def SQLSetGuildPrefixFunc(id: Rep[Long]) = {
		DBWrapper.guilds
			.filter(_.id === id)
			.map(_.commandPrefix)
	}
	private val SQLSetGuildPrefix = Compiled(SQLSetGuildPrefixFunc _)

	private def SQLSetGuildDefaultRoleFunc(id: Rep[Long]) = {
		DBWrapper.guilds
			.filter(_.id === id)
			.map(_.defaultRole)
	}
	private val SQLSetGuildDefaultRole = Compiled(SQLSetGuildDefaultRoleFunc _)

	private def SQLSetGuildAdminRoleFunc(id: Rep[Long]) = {
		DBWrapper.guilds
			.filter(_.id === id)
			.map(_.adminRole)
	}
	private val SQLSetGuildAdminRole = Compiled(SQLSetGuildAdminRoleFunc _)

	override def receive: Receive = {
		case Client.Run => run()
		case e: MessageCreateEvent => messageReceived(e)
		case e: GuildCreateEvent => guildCreated(e.getGuild.getId.asLong)
		case e: GuildDeleteEvent => guildRemoved(e.getGuildId.asLong)
		case e: MemberJoinEvent => memberAdded(e)
		case ac: Client.AddCommand => dispatcher ! ac
		case adc: Client.AddAdminCommand => dispatcher ! adc
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

		case Client.Invalidate(replyChannel) =>
			refreshGuilds(replyChannel)
			commands.registry.rangeUtils.normalizeAll()

		case Client.SetGuildPrefix(id, prefix) =>
			DBWrapper.database.run(SQLSetGuildPrefix(id.asLong()).update(prefix))
			guildInfos.update(id.asLong, guildInfos(id.asLong).copy(prefix = prefix))

		case Client.SetDefaultRole(guildSnowflake, roleSnowflake) =>
			val guildID = guildSnowflake.asLong()
			val roleID = roleSnowflake.map(_.asLong())
			DBWrapper.database.run(SQLSetGuildDefaultRole(guildID).update(roleID.getOrElse(0)))
			guildInfos.update(guildID, guildInfos(guildID).copy(defaultRole = roleID))

		case Client.SetAdminRole(guildSnowflake, roleSnowflake) =>
			val guildID = guildSnowflake.asLong()
			val roleID = roleSnowflake.asLong()
			DBWrapper.database.run(SQLSetGuildAdminRole(guildID).update(roleID))
			guildInfos.update(guildID, guildInfos(guildID).copy(adminRole = roleID))
	}
}
object Client {
	case object Run
	sealed case class AddCommand(name: String, action: Command)
	sealed case class AddCommandActor(name: String, actor: ActorRef)
	sealed case class AddAdminCommand(name: String, action: Command)
	sealed case class RemoveCommand(name: String)
	sealed case class DispatchCommand(name: String, bundle: MessageBundle)
	sealed case class DispatchCommandAdmin(name: String, bundle: MessageBundle)
	sealed case class UnknownCommand(name: String, channel: Mono[MessageChannel])
	sealed case class InsufficientPerms(name: String, channel: Mono[MessageChannel])
	sealed case class SendCommand(guildID: Snowflake, channel: Mono[MessageChannel], text: String)
	sealed case class Invalidate(replyChannel: Mono[MessageChannel])
	sealed case class SetGuildPrefix(guildID: Snowflake, prefix: String)
	sealed case class SetDefaultRole(guildId: Snowflake, roleID: Option[Snowflake])
	sealed case class SetAdminRole(guildID: Snowflake, roleID: Snowflake)

	def apply(actorSystem: ActorSystem, apiKey: String): ActorRef = {
		val iClient = new DiscordClientBuilder(apiKey).build()
		val client = actorSystem.actorOf(Props(classOf[Client], iClient))
		iClient.getEventDispatcher.on(classOf[MessageCreateEvent]).subscribe(client ! _)
		iClient.getEventDispatcher.on(classOf[GuildCreateEvent]).subscribe(client ! _)
		iClient.getEventDispatcher.on(classOf[GuildDeleteEvent]).subscribe(client ! _)
		iClient.getEventDispatcher.on(classOf[MemberJoinEvent]).subscribe(client ! _)
		client
	}
}
