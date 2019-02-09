package com.cwfitz.the_station_bot

import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

object ReportingFuture {
	def apply[R](logger: Logger)(func: => R)(implicit ec: ExecutionContext): Unit = {
		Future{ func }.failed.foreach(exception => logger.warn(s"Uncaught exception: $exception"))
	}
}
