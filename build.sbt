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
			"com.typesafe.akka" %% "akka-actor" % "2.5.19",
		    "com.typesafe.slick" %% "slick" % "3.2.3",
		    "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
		    "com.github.tminglei" %% "slick-pg" % "0.17.0",
		    "org.postgresql" % "postgresql" % "42.2.5",
		    "com.jcraft" % "jsch" % "0.1.55",
		    "com.lihaoyi" %% "fastparse" % "2.1.0",
		    "me.xdrop" % "fuzzywuzzy" % "1.2.0",
	    ),
	    scalacOptions ++= Seq(
		    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
		    "-encoding", "utf-8",                // Specify character encoding used by source files.
		    "-explaintypes",                     // Explain type errors in more detail.
		    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
		    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
		    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
		    "-language:higherKinds",             // Allow higher-kinded types
		    "-language:implicitConversions",     // Allow definition of implicit functions called views
		    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
		    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
		    "-Xfuture",                          // Turn on future language features.
		    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
		    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
		    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
		    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
		    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
		    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
		    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
		    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
		    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
		    "-Xlint:option-implicit",            // Option.apply used implicit view.
		    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
		    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
		    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
		    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
		    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
		    "-Ypartial-unification",             // Enable partial unification in type constructor inference
		    "-Ywarn-dead-code",                  // Warn when dead code is identified.
		    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
		    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
		    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
		    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
		    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
		    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
		    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
		    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
		    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
		    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
		    "-Ywarn-unused:privates",            // Warn if a private member is unused.
	    ),
	    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports"),
	    updateOptions := updateOptions.value.withLatestSnapshots(false),
	    assemblyJarName in assembly := "the-station-bot.jar",
	    assemblyMergeStrategy in assembly := {
		    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
		    case x => (assemblyMergeStrategy in assembly).value(x)
	    },
	    mainClass in assembly := Some("com.cwfitz.the_station_bot.Main")
    ))
