package com.cwfitz

import akka.actor.ActorRef
import discord4j.core.event.domain.message.MessageCreateEvent

package object the_station_bot {
	type Command = (ActorRef, MessageCreateEvent, String, ArgParser.Argument) => Unit
	case class MessageBundle(client: ActorRef, event: MessageCreateEvent, command: String, args: ArgParser.Argument)
}
