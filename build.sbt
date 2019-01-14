resolvers += "jcenter" at "http://jcenter.bintray.com"

lazy val the_station_bot = Project("the-station-bot", file("."))
    .settings(Seq(
	    name := "the-station-bot",
	    version := "0.1",
	    scalaVersion := "2.12.8",
	    libraryDependencies ++= Seq(
		    "com.discord4j" % "Discord4J" % "2.10.1",
	        "ch.qos.logback" % "logback-classic" % "1.2.3",
			"com.lihaoyi" %% "fastparse" % "2.1.0"
	    ),
	    assemblyJarName in assembly := "the-station-bot.jar",
	    mainClass in assembly := Some("com.cwfitz.the_station_bot.Main")
    ))
