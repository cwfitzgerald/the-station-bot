package com.cwfitz.the_station_bot.commands.registry

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Command
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object cars extends Command {
	private def carExists(carType: String): Future[Boolean] = {
		val query = DBWrapper.carTypes
    		.filter(_.carType === carType)
    		.exists
			.result
		DBWrapper.database.run(query)
	}

	private def addCars(channel: Mono[MessageChannel], carType: String, numbers: Seq[Int]): Unit = {
		val update = DBWrapper.carNumbers
			.map(c => (c.system, c.number, c.carType))
			.insertOrUpdateAll(numbers.map((0, _, carType)))

		DBWrapper.database.run(update).onComplete { t => t.toEither match {
			case Right(Some(value)) =>
				rangeUtils.normalize(carType)
				val msg = s"Added $value $carType cars."
				logger.info(msg)
				channel.flatMap {
					_.createMessage(msg).toScala
				}.subscribe()
			case Right(None) =>
				rangeUtils.normalize(carType)
				val msg = s"Added 0 $carType cars."
				logger.info(msg)
				channel.flatMap {
					_.createMessage(msg).toScala
				}.subscribe()
			case Left(exception) => logger.warn("Exception while adding cars: ", exception)
		}}
	}
	private def removeCars(channel: Mono[MessageChannel], carType: String, numbers: Seq[(Int, Int)]): Unit = {
		val operations = numbers.map{case (nStart, nEnd) => DBWrapper.carNumbers.filter(car => car.number >= nStart && car.number <= nEnd && car.carType === carType).delete}
		val removal = DBIO.sequence(operations)

		DBWrapper.database.run(removal).onComplete { t => t.toEither match {
			case Right(removedRowsArray) =>
				rangeUtils.normalize(carType)
				val msg = s"Removed ${removedRowsArray.sum} $carType cars."
				logger.info(msg)
				channel.flatMap {
					_.createMessage(msg).toScala
				}.subscribe()
			case Left(exception) => logger.warn("Exception while removing cars: ", exception)
		}}
	}

	private val logger = LoggerFactory.getLogger(getClass)
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		val arguments = args.split(" ")
		val response = arguments.length match {
			case 0 => Some("No car type given.")
			case 1 => Some("No ranges given.")
			case _ =>
				val carType = arguments(0)
				val carTypeExistsFuture = carExists(carType)
				val rangesStr = arguments.drop(1)
				val rangesOp = rangesStr.map(rangeUtils.parseRange)
				val noneIndex = rangesOp.indexOf(None)
				val allValid = noneIndex == -1
				if (allValid) {
					val ranges = rangesOp.map(_.get)
					val carTypeExists = Await.result(carTypeExistsFuture, Duration.Inf)
					if (carTypeExists) {
						if (command == "addcars") {
							val carNums = ranges.flatMap{case (start, end) => start to end}
							addCars(e.getMessage.getChannel.toScala, carType, carNums)
						}
						else {
							removeCars(e.getMessage.getChannel.toScala, carType, ranges)
						}
						None
					}
					else {
						Some(s"Car type $carType doesn't exist.")
					}
				}
				else {
					Some(s""""${rangesStr(noneIndex)}" is not a valid range.""")
				}
		}
		response match {
			case Some(text) =>
				e.getMessage.getChannel.toScala.flatMap {
					_.createMessage(text).toScala
				}.subscribe()
			case None =>
		}
	}
}
