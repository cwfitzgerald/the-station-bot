package com.cwfitz.the_station_bot.commands.registry

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Command
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object lookup extends Command{
	private val logger = LoggerFactory.getLogger(getClass)

	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		Try { args.split(" ")(0).toInt }.toOption match {
			case Some(number) =>
				val q = DBWrapper.carNumbers
    				.filter(car => car.system === 0 && car.number === number)
    				.join(DBWrapper.carTypes)
    				.map{case (car, carType) => (car.carTypeName, carType.manufacturer, carType.length, carType.comments)}
    				.take(1)
    				.result
				DBWrapper.database.run(q).onComplete { carSeq =>
					carSeq.toEither match {
						case Right(seq) =>
							val response = seq.length match {
								case 0 => s"No car found."
								case 1 =>
									val (typeName, manufacturer, length, comments) = seq.head
									val commentText = comments.map(c => s""""$c"""").mkString(", ")
									f"""$number is a $typeName made by $manufacturer. It is $length%.1fft long with comments: $commentText"""
								case _ => "Something's broken."
							}
							e.getMessage.getChannel.toScala.flatMap {
								chan => chan.createMessage(response).toScala
							}.subscribe()
						case Left(exception) => logger.warn("Request failed.", exception)
					}
				}
			case None =>
				e.getMessage.getChannel.toScala.flatMap {
					chan => chan.createMessage("That isn't a number bruh.").toScala
				}.subscribe()
		}
	}
}
