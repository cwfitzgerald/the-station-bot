package com.cwfitz.the_station_bot.commands.registry

import fastparse._
import SingleLineWhitespace._
import com.cwfitz.the_station_bot.database.DBWrapper
import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits._

object rangeUtils {
	private val logger = LoggerFactory.getLogger(getClass)

	@tailrec
	private def extractPairsImpl(ids: List[Int], start: Int, result: List[(Int, Int)]): List[(Int, Int)] = {
		ids match {
			case one :: two :: list =>
				val continueStreak = one + 1 == two
				val newStart = if(continueStreak) start else two
				val newResult = if(continueStreak) result else (start, one) :: result
				extractPairsImpl(two :: list, newStart, newResult)
			case one :: Nil =>
				(start, one) :: result
			case Nil =>
				(start, start) :: Nil
		}
	}

	private def extractPairs(ids: List[Int]): List[(Int, Int)] = {
		ids match {
			case first :: lst => extractPairsImpl(lst, first, Nil).reverse
			case Nil => Nil
		}
	}

	def pairToString(pair: (Int, Int)): String = pair match {
		case (left, right) => if (left == right) left.toString else s"$left-$right"
	}

	private def SQLNormalizeReadFunc(carType: Rep[String]) = {
		DBWrapper.carNumbers
			.filter(_.carType === carType)
			.map(_.number)
			.sorted
	}
	private def SQLNormalizeWriteFunc(carType: Rep[String]) = {
		DBWrapper.carTypes
			.filter(_.carType === carType)
			.map(_.numberRanges)
	}
	private val SQLNormalizeRead = Compiled(SQLNormalizeReadFunc _)
	private val SQLNormalizeWrite = Compiled(SQLNormalizeWriteFunc _)

	def normalize(carType: String): Unit = {
		logger.info(s"Normalizing $carType.")
		val readQuery = SQLNormalizeRead(carType).result
		DBWrapper.database.run(readQuery).onComplete { t1 => t1.toEither match {
			case Right(result) =>
				logger.info(s"Got ${result.length} $carType cars.")
				val pairs = extractPairs(result.toList)
    				.map(pairToString)
				val writeQuery = SQLNormalizeWrite(carType).update(pairs)
				DBWrapper.database.run(writeQuery).onComplete{ t2 => t2.toEither match {
					case Right(_) =>
						logger.info(s"Normalized $carType with ${pairs.length} ranges.")
					case Left(exception) =>
						logger.warn(s"Exception while updating $carType's car ranges: ", exception)
				}}
			case Left(exception) =>
				logger.warn(s"Exception while extracting $carType's car numbers: ", exception)
		}}
	}

	private val SQLGetAllTypes =
		DBWrapper.carTypes
			.map(_.carType)
    	    .result

	def normalizeAll(): Unit = {
		DBWrapper.database.run(SQLGetAllTypes).onComplete { t => t.toEither match {
			case Right(types) => types.foreach(normalize)
			case Left(exception) => logger.warn("Exception when pulling in car types: ", exception)
		}}
	}

	private def parseRangeImpl[_: P] = P( CharIn("0-9").rep(min = 1, max = 4).! ~ (CharIn("\\-–") ~ CharIn("0-9").rep(min = 1, max = 4).! ).? ~ End )
    	.map{
		    case(left, right) =>
			    val leftNum = left.toInt
			    val rightNum = right.map(_.toInt)
			    (leftNum, rightNum.getOrElse(leftNum))
	    }

	def parseRange(input: String): Option[(Int, Int)] = {
		parse(input, parseRangeImpl(_), verboseFailures = true) match {
			case Parsed.Success(value, _) => Some(value)
			case Parsed.Failure(label, idx, _) =>
				logger.warn(s"""Range parsing failed: "$input". Expected $label at char $idx.""")
				None
		}
	}
}
