package com.cwfitz.the_station_bot

object Main {
	def main(args: Array[String]): Unit = {
		val apiKey = sys.env.get("API_KEY") match {
			case Some(v) => v
			case None =>
				println("Cannot find API_KEY.")
				throw new IllegalArgumentException("Cannot find API_KEY")
		}

		val client = Client(apiKey)


	}
}
