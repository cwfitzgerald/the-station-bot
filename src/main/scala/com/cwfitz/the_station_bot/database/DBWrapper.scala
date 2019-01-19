package com.cwfitz.the_station_bot.database

import PostgresProfile.api._
import com.jcraft.jsch.{JSch, Session}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class DBWrapper(sshSession: Option[Session], database: Database) {
}
object DBWrapper {
	// Definition of the SUPPLIERS table
	class Guilds(tag: Tag) extends Table[(Int, String, String, String)](tag, "GUILDS") {
		def id = column[Int]("GUILD_ID", O.PrimaryKey) // This is the primary key column
		def commandPrefix = column[String]("COMMAND_PREFIX")
		def defaultRole = column[String]("DEFAULT_ROLE")
		def adminRole = column[String]("ADMIN_ROLE")
		// Every table needs a * projection with the same type as the table's type parameter
		def * = (id, commandPrefix, defaultRole, adminRole)
	}
	val guilds = TableQuery[Guilds]

	def apply(): DBWrapper = {
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

		val username = sys.env("CWF_USER")
		val password = sys.env("CWF_PASS")

		val database = Database.forURL(s"jdbc:postgresql://localhost:5432/the_station_bot?user=$username&password=$password")
		val setup = DBIO.seq(
			guilds.schema.truncate
		)
		database.run(setup)
		val reader = for (guild <- guilds) yield guild

		database.stream(reader.result).foreach({
			case (id, prefix, role, admin) => println(s"Guild: $id. Prefix: $prefix. Role: $role. Admin: $admin")
		})
		new DBWrapper(sshSession, database)
	}
}
