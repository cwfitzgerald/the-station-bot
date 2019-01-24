package com.cwfitz.the_station_bot.commands

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.{ArgParser, ChannelExtractor, Command, EmojiFilter}
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent

object emoji extends Command {
	override def apply(v1: ActorRef, v2: MessageCreateEvent, command: String, argPack: ArgParser.Argument): Unit = {
		val arg = argPack.fullText
		val string = if(arg.isEmpty) {
			"Give me somethin' to do 'ere."
		}
		else {
			command match {
				case "trainspeak" => EmojiFilter.trainspeak(ChannelExtractor.remove(arg))
				case "emoji" => EmojiFilter.emojiSpeak(EmojiFilter.trainspeak(ChannelExtractor.remove(arg)))
			}
		}

		val channelList = ChannelExtractor.find(arg).take(1)
		val channelNumber = channelList.headOption

		val channel = channelNumber match {
			case Some(n) => Snowflake.of(n)
			case None => v2.getMessage.getChannelId
		}

		v2.getClient.getChannelById(channel).toScala.ofType(classOf[TextChannel]).flatMap {
			_.createMessage(string).toScala
		}.subscribe
	}
}
