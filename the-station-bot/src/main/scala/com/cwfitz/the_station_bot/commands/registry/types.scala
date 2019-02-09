package com.cwfitz.the_station_bot.commands.registry

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.{ArgParser, Command}
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Try

object types extends Command{
	private val logger = LoggerFactory.getLogger(getClass)

	private def addType(channel: Mono[MessageChannel],
	                    carType: String,
	                    manufacturer: String,
	                    length: String,
	                    width: String,
	                    height: String,
	                    yearsBuilt: String): Unit = {
		val lengthEither = Try{ length.toDouble }.toEither
		val widthEither = lengthEither.flatMap(_ => Try{ width.toDouble }.toEither)
		val heightEither = widthEither.flatMap(_ => Try{ height.toDouble }.toEither)
		heightEither match {
			case Left(exception) =>
				logger.info("Parsing of number failed: ", exception)
				channel.flatMap {
					_.createMessage("That's not a number!").toScala
				}.subscribe()
			case Right(_) =>
				val q = DBWrapper.carTypes
					.map(c => (c.carType, c.manufacturer, c.length, c.width, c.height, c.yearsBuilt))
					.insertOrUpdate((carType, manufacturer, lengthEither.getOrElse(0), widthEither.getOrElse(0), heightEither.getOrElse(0), yearsBuilt))
				DBWrapper.database.run(q).onComplete { t => t.toEither match {
					case Right(_) =>
						channel.flatMap {
							_.createMessage(s"Added $carType!").toScala
						}.subscribe()
					case Left(exception) =>
						logger.warn(s"Exception when adding car type $carType: ", exception)
						channel.flatMap {
							_.createMessage(s"Error: ${exception.getMessage}.").toScala
						}.subscribe()
				}}
		}
	}

	private def SQLRemoveTypeFunc(carType: Rep[String]) = {
		DBWrapper.carTypes
			.filter(_.carType === carType)
	}
	private val SQLRemoveType = Compiled(SQLRemoveTypeFunc _)

	private def removeType(channel: Mono[MessageChannel], carType: String): Unit = {
		val deleteQuery = SQLRemoveType(carType).delete
		DBWrapper.database.run(deleteQuery).onComplete{ t =>
			val response = t.toEither match {
				case Right(rowsDeleted) =>
					s"Deleted $rowsDeleted car types."
				case Left(exception) =>
					logger.warn(s"Exception while deleting car type $carType: ", exception)
					s"Error."
			}
			channel.flatMap {
				chan => chan.createMessage(response).toScala
			}.subscribe()
		}
	}

	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: ArgParser.Argument): Unit = {
		val argArray = args.argv
		val channel = e.getMessage.getChannel.toScala
		if(command == "remtype" && argArray.nonEmpty) {
			removeType(channel, argArray.head)
		}
		else if (command == "addtype" && argArray.length >= 6) {
			addType(channel, argArray.head, argArray(1), argArray(2), argArray(3), argArray(4), argArray(5))
		}
		else {
			channel.flatMap {
				_.createMessage(s"Not enough arguments for $command.").toScala
			}.subscribe()
		}
	}
}
