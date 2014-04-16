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
    mappings in (Compile, packageBin) ~= { _.filterNot { case (_, name) => // TODO not working, fix
      Seq("/bin").contains(name)
    }},
    mappings in Universal <+= (packageBin in Compile) map { jar =>
      jar -> ("lib/" + jar.getName)
    },
    packagedArtifacts in Universal ~= { _.filterNot { case (artifact, file) => artifact.`type`.contains("zip")}},
    publish <<= publish.dependsOn(publish in Universal),
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
      publish := { }
    ),
    aggregate = Seq(common, special)
  )

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = defaultSettings ++ Seq(
      publish := { },
      libraryDependencies ++= Dependencies.common
    )
  )

  lazy val special = Project(
    id = "special",
    base = file("special"),
    settings = defaultSettings ++ myPackagerSettings ++ Seq(
      name in Universal := name.value + "_" + scalaBinaryVersion.value, // set artifactId as expected
      mappings in Universal ++= directory("special/src/main/webapp"),
      libraryDependencies ++= Dependencies.special
    )
  ) dependsOn (common)

}
