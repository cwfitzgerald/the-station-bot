package com.cwfitz.the_station_bot.commands.registry

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.Command
import com.cwfitz.the_station_bot.D4JImplicits._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import org.slf4j.LoggerFactory
import reactor.core.scala.publisher.Mono

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object lookup extends Command{
	private val logger = LoggerFactory.getLogger(getClass)

	private def displaySingleCar(channel: Mono[MessageChannel], carNumber: Int): Unit = {
		logger.info(s"Getting $carNumber's information")
		val q = DBWrapper.carNumbers
			.filter(c => c.system === 0 && c.number === carNumber)
			.join(DBWrapper.carTypes)
    		.filter{case (car, carTypes) => car.carType === carTypes.carType}
			.map{case (car, carTypes) => (car.carType, carTypes.manufacturer, carTypes.yearsBuilt, car.comments, car.images)}
    		.result
		DBWrapper.database.run(q).onComplete {
			case Success(carSeq) if carSeq.nonEmpty =>
				val (carType, manufacturer, yearsBuilt, comments, _) = carSeq.head
				val commentTitle = comments match {
					case Some(commentArray) =>
						commentArray.size match {
							case 0 => "No Comments"
							case 1 => "Comment"
							case _ => "Comments"
						}
					case None =>
						"No Comments"
				}
				val commentText = comments.map(_.mkString("\n") + "\u200B").getOrElse("\u200B")

				def singleCarEmbed(embed: EmbedCreateSpec): Unit = embed
					.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
					.setTitle(carNumber.toString)
					.addField("Type", carType, true)
					.addField("Manufacturer", manufacturer, true)
					.addField("Year(s) Built", yearsBuilt, true)
					.addField(commentTitle, commentText, true)
					.addField("Images", "Not yet", true)

				channel.flatMap {
					//noinspection ConvertibleToMethodValue
					_.createMessage(m => m.setEmbed(singleCarEmbed(_))).toScala
				}.subscribe()
			case Success(carSeq) =>
				channel.flatMap {
					//noinspection ConvertibleToMethodValue
					_.createMessage(s"Car $carNumber not found.").toScala
				}.subscribe()
			case Failure(exception) =>
				logger.warn(s"Exception thrown when accessing car $carNumber information: ", exception)
				channel.flatMap {
					_.createMessage(s"Error: ${exception.getMessage}").toScala
				}.subscribe()
		}
	}

	@tailrec
	private def extractCarInfosImpl(input: List[(Int, String, String)],
	                        start: Int,
	                        last: Int,
	                        inCarType: String,
	                        inManufacturer: String,
	                        result: List[(Int, Int, String, String)]): List[(Int, Int, String, String)]  = {
		input match {
			case Nil => (start, last, inCarType, inManufacturer) :: result
			case (number, carType, manufacturer) :: list =>
				val same = number == last + 1 && carType == inCarType && manufacturer == inManufacturer
				val newStart = if (same) start else number
				val res = if (same) {
					result
				}
				else {
					(start, last, inCarType, inManufacturer) :: result
				}
				extractCarInfosImpl(list, newStart, number, carType, manufacturer, res)
		}
	}

	private def extractCarInfo(input: List[(Int, String, String)]): List[(Int, Int, String, String)] = {
		(input match {
			case Nil => Nil
			case (number, carType, manufacturer) :: Nil =>
				(number, number, carType, manufacturer) :: Nil
			case (number, carType, manufacturer) :: list =>
				extractCarInfosImpl(list, number, number, carType, manufacturer, Nil)
		}).reverse
	}

	private def displayMultipleCars(channel: Mono[MessageChannel], carPairs: Seq[(Int, Int)]): Unit = {
		logger.info("Getting multicar information")
		val q = DBWrapper.carNumbers
    		.filter {
			    car => carPairs
				    .map{case(start, end) => car.number >= start && car.number <= end}
				    .reduceLeft(_ || _)
		    }
    		.join(DBWrapper.carTypes)
    		.filter{ case(car, carType) => car.carType === carType.carType }
    		.map{ case(car, carType) => (car.number, car.carType, carType.manufacturer) }
    		.result
		DBWrapper.database.run(q).onComplete {
			case Success(carSeq) if carSeq.nonEmpty  =>
				val sorted = carSeq.sorted.toList
				val carInfo = extractCarInfo(sorted)
				val carText = carInfo
					.map{case(start, end, carType, manufacturer) => s"$start-$end: $carType ($manufacturer)"}
					.mkString("\n")

				def multiCarEmbed(embed: EmbedCreateSpec): Unit = embed
					.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
					.setTitle("Multiple Cars")
					.addField("Info", carText, false)

				channel.flatMap {
					//noinspection ConvertibleToMethodValue
					_.createMessage(m => m.setEmbed(multiCarEmbed(_))).toScala
				}.subscribe()
			case Success(_) =>
				channel.flatMap {
					//noinspection ConvertibleToMethodValue
					_.createMessage(s"Cars not found.").toScala
				}.subscribe()
			case Failure(exception) =>
				logger.warn(s"Exception thrown when accessing multicar information: ", exception)
				channel.flatMap {
					_.createMessage(s"Error: ${exception.getMessage}").toScala
				}.subscribe()
		}
	}

	private def displayCars(channel: Mono[MessageChannel], carPairs: Seq[(Int, Int)]): Unit = {
		carPairs.length match {
			case 0 =>
				channel.flatMap {
					_.createMessage("No readable number pairs given!").toScala
				}.subscribe()
			case 1 if carPairs.head._1 == carPairs.head._2 => 
				displaySingleCar(channel, carPairs.head._1)
			case _ => displayMultipleCars(channel, carPairs)
		}
	}

	private def displayCarType(channel: Mono[MessageChannel], carType: String): Unit = {
		val q = DBWrapper.carTypes
    		.filter(_.carType === carType)
    		.result
		DBWrapper.database.run(q).onComplete {
			case Success(list) if list.nonEmpty =>
				val (_, numberRanges, manufacturer, length, width, height, yearsBuilt, comments, _) = list.head
				val commentTitle = comments match {
					case Some(commentArray) =>
						commentArray.size match {
							case 0 => "No Comments"
							case 1 => "Comment"
							case _ => "Comments"
						}
					case None =>
						"No Comments"
				}
				val commentText = comments.map(_.mkString("\n") + "\u200B").getOrElse("\u200B")

				def carTypeEmbed(embed: EmbedCreateSpec): Unit = embed
					.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
					.setTitle(carType)
					.addField("Numbers:", numberRanges.mkString("\n"), true)
					.addField("Manufacturer:", manufacturer, true)
					.addField("Dimensions:", f"$length%.1fx$width%.1fx$height%.1fft", true)
					.addField("Year(s) Built", yearsBuilt, true)
					.addField(commentTitle, commentText, true)
					.addField("Images:", "Not yet", true)

				channel.flatMap {
					//noinspection ConvertibleToMethodValue
					_.createMessage(m => m.setEmbed(carTypeEmbed(_))).toScala
				}.subscribe()
			case Failure(exception) =>
				logger.warn("Exception thrown when accessing car type information: ", exception)
				channel.flatMap {
					_.createMessage(s"Error: ${exception.getMessage}").toScala
				}.subscribe()
		}
	}

	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: String): Unit = {
		val arguments = args.split(" ")
		val argument = arguments.headOption
		val channel = e.getMessage.getChannel.toScala
		argument match {
			case Some(name) =>
				// First search for car type
				val q = DBWrapper.carTypes
					.filter(_.carType === name)
    				.exists
					.result
				DBWrapper.database.run(q).onComplete {
					// Is a car type
					case Success(true) =>
    					displayCarType(channel, name)
					case Success(false) =>
						val pairs = arguments.map(rangeUtils.parseRange).filter(_.nonEmpty).map(_.get)
						displayCars(channel, pairs)
					case Failure(exception) =>
						logger.warn(s"Exception thrown while determining if $name is car type: ", exception)
						channel.flatMap {
							_.createMessage(s"Error: ${exception.getMessage}").toScala
						}.subscribe()
				}
			case None =>
				channel.flatMap {
					_.createMessage(s"Nothing to look up!").toScala
				}.subscribe()
		}
	}
}
