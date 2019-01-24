package com.cwfitz.the_station_bot.database

import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import com.jcraft.jsch.JSch

object DBWrapper {
	class Guilds(tag: Tag) extends Table[(Long, String, Long, Long)](tag, "GUILDS") {
		def id = column[Long]("GUILD_ID")
		def commandPrefix = column[String]("COMMAND_PREFIX")
		def defaultRole = column[Long]("DEFAULT_ROLE")
		def adminRole = column[Long]("ADMIN_ROLE")
		// Every table needs a * projection with the same type as the table's type parameter
		def * = (id, commandPrefix, defaultRole, adminRole)
		def pk = primaryKey("GUILDS_pkey", id)
	}
	val guilds = TableQuery[Guilds]

	class CarNumbers(tag: Tag) extends Table[(Int, Int, String, Option[List[String]], Option[List[String]])](tag, "car_numbers") {
		def system = column[Int]("system")
		def number = column[Int]("number")
		def carType = column[String]("type")
		def comments = column[Option[List[String]]]("comments")
		def images = column[Option[List[String]]]("img")
		def carTypeFK = foreignKey("car_numbers_car_types_type_fk", carType, carTypes)(_.carType)
		def * = (system, number, carType, comments, images)
		def pk = primaryKey("car_numbers_pk", (system, number))
	}
	val carNumbers = TableQuery[CarNumbers]

	class CarTypes(tag: Tag) extends Table[(String, List[String], String, Double, Double, Double, String, Option[List[String]], Option[List[String]])](tag, "car_types") {
		def carType = column[String]("type")
		def numberRanges = column[List[String]]("number_ranges")
		def manufacturer = column[String]("manufacturer")
		def length = column[Double]("length")
		def width = column[Double]("width")
		def height = column[Double]("height")
		def yearsBuilt = column[String]("years_built")
		def comments = column[Option[List[String]]]("comments")
		def images = column[Option[List[String]]]("img")
		def * = (carType, numberRanges, manufacturer, length, width, height, yearsBuilt, comments, images)
		def pk = primaryKey("car_types_pk", carType)
	}
	val carTypes = TableQuery[CarTypes]

	val (sshSession, database) = {
		val usingSSH = sys.env.get("CWF_USE_SSH")
		val sshSession = usingSSH.map(_ => {
			val j = new JSch()
			j.addIdentity("~/.ssh/id_rsa")
			val sess = j.getSession("cwfitz.com")
			sess.setConfig("StrictHostKeyChecking", "no")
			sess.connect()
			sess.setPortForwardingL(5432, "localhost", 5432)
			assert(sess.isConnected)
			sess
		})

		val database = Database.forConfig("centralDB")
		(sshSession, database)
	}
}
