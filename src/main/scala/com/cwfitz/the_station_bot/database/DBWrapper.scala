package com.cwfitz.the_station_bot.database

import com.cwfitz.the_station_bot.database.PostgresProfile.api._
import com.jcraft.jsch.JSch

object DBWrapper {
	// Definition of the SUPPLIERS table
	class Guilds(tag: Tag) extends Table[(Long, String, Long, Long)](tag, "GUILDS") {
		def id = column[Long]("GUILD_ID", O.PrimaryKey) // This is the primary key column
		def commandPrefix = column[String]("COMMAND_PREFIX")
		def defaultRole = column[Long]("DEFAULT_ROLE")
		def adminRole = column[Long]("ADMIN_ROLE")
		// Every table needs a * projection with the same type as the table's type parameter
		def * = (id, commandPrefix, defaultRole, adminRole)
	}
	val guilds = TableQuery[Guilds]

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

		val username = sys.env("CWF_USER")
		val password = sys.env("CWF_PASS")

		val database = Database.forURL(s"jdbc:postgresql://localhost:5432/the_station_bot?user=$username&password=$password")
		(sshSession, database)
	}
}
