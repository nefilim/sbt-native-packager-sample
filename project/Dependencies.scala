import sbt._

object Dependencies {

  val resolutionRepos = Seq(
  )

  object V {
    val akka = "2.3.2"
    val typesafeConfig = "1.2.0"
  }

  object Libraries {
    val akka = "com.typesafe.akka"           %% "akka-actor"    % V.akka
    val akkaTestKit = "com.typesafe.akka"    %% "akka-testkit"  % V.akka % "test"
    val typesafeConfig = "com.typesafe"      % "config"         % V.typesafeConfig
  }

  import Libraries._

  val common = Seq(akka)
  val special = Seq(akka, akkaTestKit, typesafeConfig)
}
