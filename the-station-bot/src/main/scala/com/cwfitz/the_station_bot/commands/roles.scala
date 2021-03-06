package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.ArgParser
import com.cwfitz.the_station_bot.D4JImplicits._
import discord4j.core.`object`.entity.Role
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.{Logger, LoggerFactory}
import reactor.core.scala.publisher.{Flux, Mono}

import scala.collection.JavaConverters._

object roles {
	val logger: Logger = LoggerFactory.getLogger(getClass)

	def add(client: ActorRef, event: MessageCreateEvent, command: String, argPack: ArgParser.Argument): Unit = {
		val args = argPack.fullText
		val routes = args.toUpperCase.split(Array(',', ' '))
		val user = event.getMember.get()
		val valid = for {
			guild <- event.getGuild.toScala.flux
			roleIds <- Flux.fromIterable(asScalaSet(guild.getRoleIds) diff asScalaSet(user.getRoleIds))
			role <- guild.getRoleById(roleIds).flux
			route <- Flux.fromArray(routes)
			if role.getName == route
		} yield (route, role)
//
		valid.flatMap(
			x => Mono.just(x)
				.flatMap({ case (_, role: Role) => user.addRole(role.getId).toScala })
				.`then`(Mono.just(x))
		).map(
			(x: (String, Role)) => x match {
				case _ @ (_, role) => role.getMention
			}
		).collectSeq
		.map (
			(mentions: Seq[String]) =>
				if (mentions.isEmpty)
					if(args.isEmpty || args.map(_.isWhitespace).reduce(_ || _))
						"I was given no roles to add."
					else
						"You already have all of those roles."
				else
					"Added: " + mentions.sorted.mkString(", ")
		)
		.flatMap(
		    str => event.getMessage.getChannel.toScala.flatMap(
			    _.createMessage(str).toScala
		    )
	    ).subscribe()
	}
	def remove(client: ActorRef, event: MessageCreateEvent, command: String, argPack: ArgParser.Argument): Unit = {
		val args = argPack.fullText
		val routes = args.toUpperCase.split(Array(',', ' '))
		val user = event.getMember.get()
		val valid: Flux[(String, Role)] = for {
			guild <- event.getGuild.toScala.flux
			roleIds <- Flux.fromIterable(asScalaSet(guild.getRoleIds) intersect asScalaSet(user.getRoleIds))
			role <- guild.getRoleById(roleIds).flux
			route <- Flux.fromArray(routes)
			if role.getName == route
		} yield (route, role)
		//
		valid.flatMap(
			x => Mono.just(x)
				.flatMap({ case (_, role: Role) => user.removeRole(role.getId).toScala })
				.`then`(Mono.just(x))
		).map(
			(x: (String, Role)) => x match {
				case (_, role) => role.getMention
			}
		).collectSeq
		.map (
			(mentions: Seq[String]) =>
				if (mentions.isEmpty)
					if(args.isEmpty || args.map(_.isWhitespace).reduce(_ || _))
						"I was given no roles to remove."
					else
						"You don't have any of those roles."
				else
					"Removed: " + mentions.sorted.mkString(", ")
		)
		.flatMap(
			str => event.getMessage.getChannel.toScala.flatMap(
				_.createMessage(str).toScala
			)
		).subscribe()
	}
}
