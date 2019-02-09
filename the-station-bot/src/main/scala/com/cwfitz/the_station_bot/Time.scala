package com.cwfitz.the_station_bot

import java.lang.System.nanoTime

object Time {
	def apply[R](expr: => R): (R, Long) = {
		val t0 = nanoTime()
		val result = expr
		val t1 = nanoTime()
		(result, t1 - t0)
	}
}
