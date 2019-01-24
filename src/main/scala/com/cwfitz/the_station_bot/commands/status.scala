package com.cwfitz.the_station_bot.commands

import java.lang.management.ManagementFactory
import java.time.Duration

import akka.actor.ActorRef
import com.cwfitz.the_station_bot.{ArgParser, Command}
import com.cwfitz.the_station_bot.D4JImplicits._
import com.sun.management.OperatingSystemMXBean
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.util.VersionUtil

object status extends Command {
	override def apply(client: ActorRef, e: MessageCreateEvent, command: String, args: ArgParser.Argument): Unit = {
		val version = VersionUtil.getProperties.getProperty(VersionUtil.GIT_COMMIT_ID_ABBREV)
		val osBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]
		val runtimeBean = ManagementFactory.getRuntimeMXBean
		val runtime = Runtime.getRuntime

		val uptime = Duration.ofMillis(runtimeBean.getUptime)
		val days = uptime.toDays
		val hours = uptime.toHours % 24
		val minutes = uptime.toMinutes % 60
		val seconds = uptime.getSeconds % 60
		val mills = uptime.toMillis % 1000

		val processors = runtime.availableProcessors()
		val jvmLoad = osBean.getProcessCpuLoad * 100
		val systemLoad = osBean.getSystemCpuLoad * 100

		val totalSystemMem = osBean.getTotalPhysicalMemorySize.toDouble / 1000000
		val totalUsedMem = totalSystemMem - (osBean.getFreePhysicalMemorySize.toDouble / 1000000)
		val totalJVMMem = runtime.totalMemory().toDouble / 1000000
		val usedJVMMem = totalJVMMem - (runtime.freeMemory().toDouble / 1000000)

		val totalBotUsed = (usedJVMMem / totalSystemMem) * 100
		val totalSystemUsed = (totalUsedMem / totalSystemMem) * 100
		val totalJVMUsed = (usedJVMMem / totalJVMMem) * 100

		def createEmbed(embed: EmbedCreateSpec): Unit = {
			embed
				.setAuthor("Andrew Cuomo", null, "https://cwfitz.com/s/_qaqGg.jpg")
				.setTitle("Current Status")
				.addField("Author", "Sirflankalot#3671", true)
    			.addField("Built For", "The Station\nhttps://discord.gg/tzP6UA3", true)
				.addField("Powered By", s"Discord4J 3.0.0-$version\non Java ${classOf[Runtime].getPackage.getSpecificationVersion}", true)
				.addField("Uptime", f"${days}d ${hours}h ${minutes}m ${seconds}s ${mills}ms", true)
				.addField("CPU Usage", f"Cores: $processors\nSystem: $systemLoad%.1f%%\nCuomo: $jvmLoad%.1f%%", true)
				.addField("Ram Usage",
					f"""System: $totalSystemUsed%.1f%% of $totalSystemMem%.0fMB
					   | Cuomo: $totalBotUsed%.1f%% of $totalSystemMem%.0fMB
					   |   JVM: $totalJVMUsed%.1f%% of $totalJVMMem%.0fMB""".stripMargin, true)
		}

		e.getMessage.getChannel.toScala.flatMap {
			chan => chan.createMessage(spec => spec.setEmbed(spec1 => createEmbed(spec1))).toScala
		}.subscribe()
	}
}
