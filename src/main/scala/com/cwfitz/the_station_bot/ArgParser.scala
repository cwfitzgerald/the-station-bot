package com.cwfitz.the_station_bot

import fastparse._
import NoWhitespace._
import org.slf4j.LoggerFactory

object ArgParser {
	private val logger = LoggerFactory.getLogger(getClass)
	final case class Argument(fullText: String, argv: Seq[String], argc: Int)

	private def str[_: P] = P( "\"" ~ ( P("\\\"".!).map(_ => "\"") | (!"\"" ~ AnyChar).! ).rep ~ "\"" )
    	.map(_.mkString)
	private def arg[_: P] = P( (!" " ~ (str | AnyChar.!)).rep() )
    	.map(_.mkString)
	private def args[_: P] = P( (!End ~ arg ~ (" " | End)).rep ~ End )

	def apply(input: String): Argument = {
		val (array, time) = Time { parse(input, args(_), verboseFailures = true) match {
			case Parsed.Success(value, _) =>
				value.filter(_.nonEmpty).map(_.trim)
			case Parsed.Failure(label, index, _) =>
				logger.warn(s"Arg splitting failed. Expected $label at index $index.")
				Seq(input)
		}}
		logger.debug(f"Args parsed in ${time / 100000.0}%.1fms")
		Argument(input.trim, array, array.length)
	}
}
