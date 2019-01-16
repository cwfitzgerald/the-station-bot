package com.cwfitz.the_station_bot

import java.util.Optional

import reactor.core.publisher
import reactor.core.scala.publisher.{Flux, Mono, PimpMyPublisher}

import scala.language.implicitConversions

object D4JImplicits {
	implicit class OptJ2S[T >: Null](val op: Optional[T]) extends AnyVal {
		def toScala: Option[T] = Option(op.orElse(null))
	}
	implicit class OptS2J[T >: Null](val op: Option[T]) extends AnyVal {
		def toJava: Optional[T] = Optional.ofNullable(op.orNull)
	}
	implicit class FluxJ2S[T](val flux: publisher.Flux[T]) extends AnyVal {
		def toScala: Flux[T] = PimpMyPublisher.jfluxToFlux(flux)
	}
	implicit class FluxS2J[T](val flux: Flux[T]) extends AnyVal {
		def toJava: publisher.Flux[T] = PimpMyPublisher.fluxToJFlux(flux)
	}
	implicit class MonoJ2S[T](val mono: publisher.Mono[T]) extends AnyVal {
		def toScala: Mono[T] = PimpMyPublisher.jMonoToMono(mono)
	}
	implicit class MonoS2J[T](val mono: Mono[T]) extends AnyVal {
		def toJava: publisher.Mono[T] = PimpMyPublisher.monoToJMono(mono)
	}

	implicit class FluxMonadic[T](val f: Flux[T]) extends AnyVal {
		def withFilter(pred: T => Boolean): Flux[T] = f.filter(pred)
		def foreach[U](func: T => U): Unit = f.subscribe(t => func(t))
		def map[U](func: T => U): Unit = f.map(func)
	}
	implicit class MonoMonadic[T](val m: Mono[T]) extends AnyVal {
		def withFilter(pred: T => Boolean): Mono[T] = m.filter(pred)
		def foreach[U](func: T => U): Unit = m.subscribe(t => func(t))
	}
}
