package com.cwfitz

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

package object the_station_bot {
	type Command = (Client, MessageReceivedEvent, String) => Unit
}
