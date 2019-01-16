package com.cwfitz

import discord4j.core.event.domain.message.MessageCreateEvent

package object the_station_bot {
	type Command = (Client, MessageCreateEvent, String) => Unit
	case class MessageBundle(c: Client, e: MessageCreateEvent, args: String)
}
