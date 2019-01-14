package com.cwfitz.the_station_bot.commands

import fastparse._, fastparse.SingleLineWhitespace._
import com.cwfitz.the_station_bot.{Client, Command, buffered}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

import scala.util.Try

object speed extends Command {
	case class ParseError(reason: String) extends Exception

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
	private def from[_: P]: P[Double] = P(IgnoreCase("from").? ~/ num ~ StringInIgnoreCase("s", "sec", "second", "seconds").? )
	private def to[_: P]: P[Double] = P(IgnoreCase("to").? ~/ num ~ StringInIgnoreCase("s", "sec", "second", "seconds").? )
	private def carType[_: P]: P[Double] = P( IgnoreCase("R") ~ CharIn("0-9").rep(min = 1, max = 3).! ~ CharIn("ABab").? ~ IgnoreCase("s").? )
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
			    case _ => throw ParseError("Unknown train car")
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

	override def apply(c: Client, e: MessageReceivedEvent, arg: String): Unit = {
		val message = Try { parse(arg, speedCalc(_), verboseFailures = true) }.toEither match {
			case Left(exception) => exception.getMessage
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

		buffered {
			e.getChannel.sendMessage(s"${e.getAuthor.mention(true)}: $message")
		}
	}
}
