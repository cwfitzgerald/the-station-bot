resolvers ++= Seq(
	"jcenter" at "http://jcenter.bintray.com",
	"jitpack.io" at "https://jitpack.io",
	"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val the_station_bot = Project("the-station-bot", file("."))
    .settings(Seq(
	    name := "the-station-bot",
	    version := "0.1",
	    scalaVersion := "2.12.8",
	    libraryDependencies ++= Seq(
		    "com.discord4j.discord4j" % "discord4j-core" % "v3-SNAPSHOT",
		    "io.projectreactor" % "reactor-scala-extensions_2.12" % "0.3.5",
	        "ch.qos.logback" % "logback-classic" % "1.2.3",
			"com.lihaoyi" %% "fastparse" % "2.1.0",
			"com.typesafe.akka" %% "akka-actor" % "2.5.19",
			"com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test
	    ),
	    updateOptions := updateOptions.value.withLatestSnapshots(false),
	    assemblyJarName in assembly := "the-station-bot.jar",
	    assemblyMergeStrategy in assembly := {
		    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
		    case x => (assemblyMergeStrategy in assembly).value(x)
	    },
	    mainClass in assembly := Some("com.cwfitz.the_station_bot.Main")
    ))
