import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.{crossProject, CrossType}

val scalazVersion = "7.2.20"

def gitHash: String = scala.util.Try(
  sys.process.Process("git rev-parse HEAD").lineStream.head
).getOrElse("master")

val unusedWarnings = (
  "-Ywarn-unused" ::
  "-Ywarn-unused-import" ::
  Nil
)

val Scala211 = "2.11.12"

lazy val buildSettings = Seq(
  BuildInfoPlugin.projectSettings,
  scalapropsCoreSettings
).flatten ++ Seq(
  name := "scala-logic",
  scalaVersion := Scala211,
  crossScalaVersions := Seq("2.10.7", Scala211, "2.12.4", "2.13.0-M3"),
  resolvers += Opts.resolver.sonatypeReleases,
  scalacOptions ++= (
    "-deprecation" ::
    "-unchecked" ::
    "-feature" ::
    "-language:existentials" ::
    "-language:higherKinds" ::
    "-language:implicitConversions" ::
    "-language:reflectiveCalls" ::
    Nil
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v >= 11 =>
        unusedWarnings
      case _ =>
        Nil
    }
  },
  scalapropsVersion := "0.5.4",
  publishTo := sonatypePublishTo.value,
  libraryDependencies ++= Seq(
    "org.scalaz" %%% "scalaz-core" % scalazVersion,
    "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion.value % "test",
    "com.github.scalaprops" %%% "scalaprops-scalazlaws" % scalapropsVersion.value % "test"
  ),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary),
  buildInfoKeys := BuildInfoKey.ofN(
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
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommandAndRemaining(s"; ++ ${Scala211} ; native/publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommandAndRemaining("sonatypeReleaseAll"),
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
) ++ Seq(Compile, Test).flatMap(c =>
  scalacOptions in (c, console) ~= {_.filterNot(unusedWarnings.toSet)}
)

lazy val logic = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    buildSettings
  )
  .jsSettings(
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/pocketberserker/scala-logic/" + gitHash
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
  )
  .nativeSettings(
    scalapropsNativeSettings,
    scalaVersion := Scala211,
    crossScalaVersions := Seq(Scala211)
  )

lazy val root = project
  .in(file("."))
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    PgpKeys.publishSigned := {},
    PgpKeys.publishLocalSigned := {},
    sources in Compile := Nil,
    sources in Test := Nil,
  )
  .aggregate(
    // // ignore native on purpose
    jvm,
    js
  )

lazy val jvm = logic.jvm.withId("jvm")
lazy val js = logic.js.withId("js")
lazy val native = logic.native.withId("native")
