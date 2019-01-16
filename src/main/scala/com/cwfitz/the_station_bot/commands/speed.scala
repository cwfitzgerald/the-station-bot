package com.cwfitz.the_station_bot.commands

import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.{Client, Command}
import discord4j.core.event.domain.message.MessageCreateEvent
import fastparse.SingleLineWhitespace._
import fastparse._
import org.slf4j.LoggerFactory

import scala.util.Try

object speed extends Command {
	private val logger = LoggerFactory.getLogger(getClass)
	case class ParseError(reason: String) extends Exception(reason)

	private def num[_: P]: P[Double] = P(CharsWhileIn("0-9").! ~ ("." ~ CharsWhileIn("0-9")).!.?).opaque("Number")
		.map {
			case (numerator, denominator) => try {
				(numerator + denominator.getOrElse(".0")).toDouble
			} catch {
				case _: NumberFormatException => throw ParseError(s"${numerator + denominator.getOrElse(".0")} is not a number.")
			}
		}
	private def distance[_: P]: P[Double] =  P( num ~ StringIn("ft", "in", "m", "mi").!).opaque("Distance")
		.map {
			case (num, "ft") => num * 0.3048
			case (num, "in") => num * 0.0254
			case (num, "mi") => num * 1609
			case (num, "m") => num
			case (_, unit) => throw ParseError(s"Invalid unit $unit")
		}
	private def timeSep[_: P]: P[Unit] = P( ",".? ~ IgnoreCase("and").?).log
	private def seconds[_: P]: P[Double] = P( num ~ StringInIgnoreCase("s", "sec", "secs", "second", "seconds").? ).log
	private def minutes[_: P]: P[Double] = P( num ~ StringInIgnoreCase("m", "min", "mins", "minute", "minutes").? ~ timeSep ).log
	private def hours[_: P]: P[Double] = P( num ~ StringInIgnoreCase("h", "hr", "hrs", "hour", "hours").? ~ timeSep ).log
	private def timeTryHours[_: P]: P[Double] = P( hours ~ minutes ~ seconds ).log
		.map {
			case (hours, minutes, seconds) => hours * 3600 + minutes * 60 + seconds
		}
	private def timeTryMinutes[_: P]: P[Double] = P( minutes ~ seconds ).log
		.map {
			case (minutes, seconds) => minutes * 60 + seconds
		}
	private def timeTrySeconds[_: P]: P[Double] = P( seconds ).log
	private def time[_: P]: P[Double] = P( timeTryHours | timeTryMinutes | timeTrySeconds ).opaque("Time").log
	private def from[_: P]: P[Double] = P(IgnoreCase("from").? ~/ time )
	private def to[_: P]: P[Double] = P(IgnoreCase("to").? ~/ time )
	private def carType[_: P]: P[Double] = P( IgnoreCase("R") ~ CharIn("0-9").repX(min = 1, max = 3).! ~ CharIn("ABab").? ~ IgnoreCase("s").? )
    	.map(str => {
		    (str.toInt match {
			    case 32 => 60
			    case 42 => 60
			    case 44 => 75
			    case 46 => 75
			    case 62 => 51
			    case 68 => 75
			    case 142 => 51
			    case 143 => 60
			    case 160 => 60
			    case 179 => 60
			    case 188 => 51
			    case 211 => 60
			    case v => throw ParseError(s"Unknown train car R$v")
			}) * 0.3048
	    })
	private def car[_: P]: P[Double] = P(carType | (distance ~ StringInIgnoreCase("er", "ers").?)).opaque("Car Type/Distance")
	private def carCount[_: P]: P[Double] = P( (num ~ "x".?) | ("x" ~ num) )
	private def over[_: P]: P[Double] = P( IgnoreCase("over").? ~ carCount.? ~ car )
    	.map {
		    case (count, length) => count.getOrElse(1.0) * length
	    }
	private def speedCalc[_: P]: P[Double] = P( over ~ from ~ to )
    	.map {
		    case (distance, start, end) => (distance / (end - start)) * 2.237
	    }

	private def getParseText(arg: String): String = {
		Try { parse(arg, speedCalc(_), verboseFailures = true) }.toEither match {
			case Left(exception) => exception match {
				case ParseError(msg) => msg
				case _ =>
					logger.warn("Unhandled exception", exception)
					s"""Internal parser error "${exception.getMessage}""""
			}
			case Right(parsed) => parsed match {
				case Parsed.Success(value, _) =>
					"That train is moving at %.1f mph!".format(value)
				case Parsed.Failure(label, index, _) =>
					val endIndex = 0.max(arg.length - 1)
					val minIndex = index.min(endIndex)
					val maxIndex = (index + 10).min(endIndex)
					s"""Parsed failed at "${arg.substring(minIndex, maxIndex)}". Expected "$label"."""
			}
		}
	}

	override def apply(c: Client, e: MessageCreateEvent, arg: String): Unit = {
		if (arg.isEmpty) {
			help.help(e, "speed")
		} else {
			val message = getParseText(arg)

			e.getMessage.getChannel.toScala.flatMap {
				chan => chan.createMessage(s"${e.getMember.get.getMention}: $message").toScala
			}.subscribe()
		}

	}
}
