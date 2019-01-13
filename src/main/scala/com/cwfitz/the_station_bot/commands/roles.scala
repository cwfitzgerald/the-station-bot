package com.cwfitz.the_station_bot.commands

import scala.collection.JavaConverters._
import com.cwfitz.the_station_bot.{Client, buffered}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object roles {
	def add(client: Client, event: MessageReceivedEvent, args: String): Unit = {
		val routes = args.toUpperCase.split(Array(',', ' '))
		val guild = event.getGuild
		val roles = collectionAsScalaIterable(guild.getRoles)
		val user = event.getAuthor
		val valid = for {
			route <- routes
			role <- roles
			if role.getName == route
			if !user.hasRole(role)
		} yield (route, role)

		valid foreach {
			case (_, role) => buffered { user.addRole(role) }
		}

		val message = if (valid.nonEmpty) {
			"Added: " + valid.map(_._2.mention()).reduce(_ + ", " + _)
		}
		else {
			"You have all of those roles."
		}

		buffered { event.getChannel.sendMessage(message) }
	}
	def remove(client: Client, event: MessageReceivedEvent, args: String): Unit = {
		val routes = args.toUpperCase.split(Array(',', ' '))
		val guild = event.getGuild
		val roles = collectionAsScalaIterable(guild.getRoles)
		val user = event.getAuthor
		val valid = for {
			route <- routes
			role <- roles
			if role.getName == route
			if user.hasRole(role)
		} yield (route, role)

		valid foreach {
			case (_, role) => buffered { user.removeRole(role) }
		}

		val message = if (valid.nonEmpty) {
			"Removed: " + valid.map(_._2.mention()).reduce(_ + ", " + _)
		}
		else {
			"You don't have those roles."
		}

		buffered { event.getChannel.sendMessage(message) }
	}
}
