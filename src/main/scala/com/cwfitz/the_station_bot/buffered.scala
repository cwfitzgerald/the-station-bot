package com.cwfitz.the_station_bot

import sx.blah.discord.util.RequestBuffer

object buffered {
	def apply[R](f: => R): Unit = RequestBuffer.request(() => { f } )
}
