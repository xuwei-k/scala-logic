import sbt._
import Keys._
import sbtrelease._
import xerial.sbt.Sonatype._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys
import sbtbuildinfo.Plugin._

object ScalaLogicBuild extends Build {
  import Dependencies._

  private def gitHash: String = scala.util.Try(
    sys.process.Process("git rev-parse HEAD").lines_!.head
  ).getOrElse("master")

  lazy val buildSettings = Seq(
    ReleasePlugin.releaseSettings,
    sonatypeSettings,
    buildInfoSettings
  ).flatten ++ Seq(
    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.10.5", scalaVersion.value),
    resolvers += Opts.resolver.sonatypeReleases,
    scalacOptions ++= (
      "-deprecation" ::
      "-unchecked" ::
      "-Xlint" ::
      "-feature" ::
      "-language:existentials" ::
      "-language:higherKinds" ::
      "-language:implicitConversions" ::
      "-language:reflectiveCalls" ::
      Nil
    ),
    scalacOptions ++= {
      if(scalaVersion.value.startsWith("2.11"))
        Seq("-Ywarn-unused", "-Ywarn-unused-import")
      else
        Nil
    },
    testFrameworks += new TestFramework("scalaz.ScalazCheckFramework"),
    libraryDependencies ++= Seq(
      scalaz,
      "com.github.xuwei-k" %% "scalazcheck" % "0.1-SNAPSHOT" % "test"
    ),
    resolvers += "bintray/non" at "http://dl.bintray.com/non/maven",
    resolvers += Opts.resolver.sonatypeSnapshots,
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.5.2"  cross CrossVersion.binary),
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      scalaVersion,
      sbtVersion,
      scalacOptions,
      licenses,
      "scalazVersion" -> scalazVersion
    ),
    buildInfoPackage := "logic",
    buildInfoObject := "BuildInfoScalaLogic",
    sourceGenerators in Compile <+= buildInfo,
    ReleasePlugin.ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(
        action = state => Project.extract(state).runTask(PgpKeys.publishSigned, state)._1,
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")){
      case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    }.toList,
    organization := "com.github.pocketberserker",
    homepage := Some(url("https://github.com/pocketberserker/scala-logic")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    pomExtra :=
      <developers>
        <developer>
          <id>pocketberserker</id>
          <name>Yuki Nakayama</name>
          <url>https://github.com/pocketberserker</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:pocketberserker/scala-logic.git</url>
        <connection>scm:git:git@github.com:pocketberserker/scala-logic.git</connection>
        <tag>{if(isSnapshot.value) gitHash else { "v" + version.value }}</tag>
      </scm>
    ,
    description := "logic programming monad for Scala",
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) = new RewriteRule {
        override def transform(n: Node) =
          if (f(n)) NodeSeq.Empty else n
      }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    }
  )

  lazy val logic = Project(
    id = "scala-logic",
    base = file("."),
    settings = buildSettings
  )

  object Dependencies {
    val scalazVersion = "7.1.1"
    val scalaz = "org.scalaz" %% "scalaz-core" % scalazVersion
  }
}
