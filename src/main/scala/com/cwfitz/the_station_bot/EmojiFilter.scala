package com.cwfitz.the_station_bot

import fastparse.NoWhitespace._
import fastparse._
import org.slf4j.LoggerFactory

import scala.collection.immutable

object EmojiFilter {
	private val logger = LoggerFactory.getLogger(getClass)

	private def trainDetect[_: P]: P[String] = P(
		StringInIgnoreCase(
			"(1)",
			"(2)",
			"(3)",
			"(4)",
			"(5)",
			"(6)",
			"(7)",
			"(9)",
			"(A)",
			"(B)",
			"(C)",
			"(D)",
			"(E)",
			"(F)",
			"(G)",
			"(H)",
			"(J)",
			"(K)",
			"(L)",
			"(M)",
			"(N)",
			"(Q)",
			"(R)",
			"(S)",
			"(T)",
			"(V)",
			"(W)",
			"(Z)",
			"(Byl)",
			"(Dyl)",
			"(Fpi)",
			"(HH)",
			"(Mbr)",
			"(Syl)",
			"(Sor)",
			"(Sbl)",
			"<4>",
			"<5>",
			"<6>",
			"<7>",
			"<A>",
			"<B>",
			"<C>",
			"<D>",
			"<J>",
			"<M>",
			"<N>",
			"<Q>",
			"<Qor>",
			"<R>",
			"<Rbr>",
			"<W>",
		).!
	).map(s => trainSpeakMap(s.toUpperCase))
	private def scanForward[_: P] = P ( CharsWhile(c => c != '(' && c != '<', 0).! )
	private def trainOrElse[_: P] = P( trainDetect | ("(" | "<").! )
	private def trainFilter[_: P] = P( scanForward ~ (trainOrElse ~ scanForward ).rep )
    	.map({
		    case (start, rest) => start + rest.map(
			    t => t._1 + t._2
		    ).mkString
	    })

	val trainSpeakMap: Map[String, String] = immutable.ListMap (
		"(1)" -> "<:1_Train:333803258718060545>",
		"(2)" -> "<:2_Train:333805419292000256>",
		"(3)" -> "<:3_Train:333805448006336512>",
		"(4)" -> "<:4_Train:333805472492683264>",
		"<4>" -> "<:4traind:485131288727257109>",
		"(5)" -> "<:5_Train:333805503430000640>",
		"<5>" -> "<:5traind:485131288874057728>",
		"(6)" -> "<:6_Train:333805527807033344>",
		"<6>" -> "<:6traind:485131555161767947>",
		"(7)" -> "<:7_Train:333805562414366720>",
		"<7>" -> "<:7traind:485131569976311808>",
		"(9)" -> "<:9_Train:333806600873836545>",
		"(A)" -> "<:A_Train:333805601618526208>",
		"<A>" -> "<:atraind:485131596488245249>",
		"(B)" -> "<:B_Train:333805631482101760>",
		"<B>" -> "<:btraind:485131614385471499>",
		"(C)" -> "<:C_Train:333805678336540683>",
		"<C>" -> "<:ctraind:485131667527303188>",
		"(D)" -> "<:D_Train:333805689958825984>",
		"<D>" -> "<:dtraind:485131690939777027>",
		"(E)" -> "<:E_Train:333805699689873408>",
		"(F)" -> "<:F_Train:333805712545284096>",
		"(G)" -> "<:G_Train:333805723278508036>",
		"(J)" -> "<:J_Train:333805748637270016>",
		"<J>" -> "<:jtraind:485131734078193664>",
		"(L)" -> "<:L_Train:333805769721905162>",
		"(M)" -> "<:M_Train:333805781550104576>",
		"<M>" -> "<:mtraind:485131751035895820>",
		"(N)" -> "<:N_Train:333805792010436618>",
		"<N>" -> "<:ntraind:485131769893617665>",
		"(Q)" -> "<:Q_Train:333805809047961601>",
		"<Q>" -> "<:qtraind:485131791720644619>",
		"(R)" -> "<:R_Train:333805829692325898>",
		"<R>" -> "<:rtraind:485131828089454605>",
		"(S)" -> "<:Shuttle:333805844263206912>",
		"(V)" -> "<:V_Train:333805875481280524>",
		"(W)" -> "<:W_Train:333805891327623179>",
		"<W>" -> "<:wtraind:485131909039390762>",
		"(Z)" -> "<:Z_Train:333805907567968257>",
		"(BYL)" -> "<:yellowb:485131650536046600>",
		"(DYL)" -> "<:yellowd:485131705481428997>",
		"(Fpk)" -> "<:pinkf:486725794664808459>",
		"(MBR)" -> "<:brownm:517375051142791184>",
		"<QOR>" -> "<:orangeq:485131801107365920>",
		"<RBR>" -> "<:brownrd:485131846070304769>",
		"(SBL)" -> "<:leffertsshuttle:485154824397127711>",
		"(SOR)" -> "<:grandshuttle:485154824191737857>",
		"(SYL)" -> "<:63shuttle:485154824455979018>",
		"(H)" -> "<:H_Train:333805732568891392>",
		"(HH)" -> "<:hhtrain:485147336843198485>",
		"(K)" -> "<:K_Train:333805759508774912>",
		"(T)" -> "<:T_Train:333805857718534144>",
	)

	def trainspeak(input: String): String = {
		Time { parse(input, trainFilter(_)) } match {
			case (Parsed.Success(value, _), time) =>
				logger.debug(f"Trainspeak converted in ${time / 1000000.0}%.2fms")
				value
			case (Parsed.Failure(label, index, _), _) =>
				logger.warn(s"""Emoji substitution failed. Input: "$input". Error "$label". Index: $index""")
				input
		}
	}

	val emojiMap: Map[Char, String] = immutable.ListMap(
		'A' -> "<:A_Train:333805601618526208>",
		'B' -> "<:B_Train:333805631482101760>",
		'C' -> "<:C_Train:333805678336540683>",
		'D' -> "<:D_Train:333805689958825984>",
		'E' -> "<:E_Train:333805699689873408>",
		'F' -> "<:F_Train:333805712545284096>",
		'G' -> "<:G_Train:333805723278508036>",
		'H' -> "<:H_Train:333805732568891392>",
		'I' -> "🇮",
		'J' -> "<:J_Train:333805748637270016>",
		'K' -> "<:K_Train:333805759508774912>",
		'L' -> "<:L_Train:333805769721905162>",
		'M' -> "<:M_Train:333805781550104576>",
		'N' -> "<:N_Train:333805792010436618>",
		'O' -> "🅾",
		'P' -> "🅿",
		'Q' -> "<:Q_Train:333805809047961601>",
		'R' -> "<:R_Train:333805829692325898>",
		'S' -> "<:Shuttle:333805844263206912>",
		'T' -> "<:T_Train:333805857718534144>",
		'U' -> "🇺",
		'V' -> "<:V_Train:333805875481280524>",
		'W' -> "<:W_Train:333805891327623179>",
		'X' -> "🇽",
		'Y' -> "🇾",
		'Z' -> "<:Z_Train:333805907567968257>",
		'1' -> "<:1_Train:333803258718060545>",
		'2' -> "<:2_Train:333805419292000256>",
		'3' -> "<:3_Train:333805448006336512>",
		'4' -> "<:4_Train:333805472492683264>",
		'5' -> "<:5_Train:333805503430000640>",
		'6' -> "<:6_Train:333805527807033344>",
		'7' -> "<:7_Train:333805562414366720>",
		'8' -> "🎱",
		'9' -> "<:9_Train:333806600873836545>",
		'0' -> "🅾"
	)

	private val emojiRegex = "<:[^:]*:[0-9]*>".r

	def emojiSpeak(input: String): String = {
		val (result, time) = Time {
			val removedText = emojiRegex.replaceAllIn(input, "\u0000")
			val emoji = emojiRegex.findAllIn(input).toArray
			val emojiText = removedText.map(c => if (c.isLetterOrDigit) emojiMap.getOrElse(c.toUpper, "🅱") else c.toString).mkString("\u200B")
			var count = 0
			emojiText.map(c => if (c == '\u0000') {
				val e = emoji(count); count += 1; e
			} else c.toString).mkString
		}
		logger.debug(f"Emojis converted in ${time / 1000000.0}%.2fms")
		result
	}
}
