package com.cwfitz.the_station_bot

import scala.util.matching.Regex

object ChannelExtractor {
	private val pattern = "<#([0-9]+)>".r

	def find(input: String): Seq[String] = {
		pattern.findAllMatchIn(input).map(_.group(1)).toSeq
	}
	def remove(input: String): String = {
		pattern.replaceAllIn(input, "")
	}
}
