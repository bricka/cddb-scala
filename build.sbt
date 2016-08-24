name := "cddb-scala"
version := "1.0"
scalaVersion := "2.11.8"

libraryDependencies ++=  Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",

  "org.mock-server" % "mockserver-netty" % "3.10.4" % Test,
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

scalastyleFailOnError := true

// Run Scalastyle with compile
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle

// Create a default Scala style task to run with tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value

(test in Test) <<= (test in Test) dependsOn testScalastyle
