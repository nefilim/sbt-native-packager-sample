import sbt._
import sbt.Keys._
import scala.Some
import com.typesafe.sbt.packager.MappingsHelper._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

object MyBuild extends Build {

  // prompt shows current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  lazy val buildSettings = Defaults.defaultSettings ++ Seq( 
    organization := "com.xxx",
    version      := "0.1.2-SNAPSHOT",
    scalaVersion := "2.10.4"
  )

  lazy val myPackagerSettings = packageArchetype.java_application ++ deploymentSettings ++ Seq(
    /* DO NOT INCLUDE THIS UPDATE TO MAPPINGS FROM THE DOCS, see https://github.com/sbt/sbt-native-packager/issues/227
    mappings in Universal <+= (packageBin in Compile) map { jar =>
      jar -> ("lib/" + jar.getName)
    }, 
    */
    packagedArtifacts in Universal ~= { _.filterNot { case (artifact, file) => artifact.`type`.contains("zip")}}, // don't publish the zip file, only the tgz, is it possible not to produce either?
    publish <<= publish.dependsOn(publish in Universal), // depends rather than replace, we still want to publish the jar,sources,javadoc and POM (if applicable) in addition to the tgz
    publishLocal <<= publishLocal.dependsOn(publishLocal in Universal)
  )

  lazy val publishSettings = Seq(
    publishTo <<= version { v: String =>
      val nexus = "https://nexus.xxx.com/nexus/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "content/repositories/releases")
    }
  )

  lazy val defaultSettings = buildSettings ++ publishSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.7", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls"),
    testOptions in Test += Tests.Argument("-oDF")
  )

  lazy val myrootProject = Project(
    id = "myroot",
    base = file("."),
    settings = defaultSettings ++ Seq(
      publish := { } // don't publish a root project artifact
    ),
    aggregate = Seq(common, special)
  )

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = defaultSettings ++ Seq(
      publish := { }, // don't publish a common artifact, it's only a dependency for other projects
      libraryDependencies ++= Dependencies.common
    )
  )

  lazy val special = Project(
    id = "special",
    base = file("special"),
    settings = defaultSettings ++ myPackagerSettings ++ Seq(
      name in Universal := name.value + "_" + scalaBinaryVersion.value, // set artifactId as "special_2.10"
      mappings in Universal ++= directory("special/src/main/webapp"), // include our webapp sources (html,js,css) in the tgz
      libraryDependencies ++= Dependencies.special
    )
  ) dependsOn (common)

}
