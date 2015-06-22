import sbt.Keys._
import sbt._
import sbtrelease._

object scalaPlayground extends Build {

  import sbtassembly.Plugin._
  import AssemblyKeys._
  import sbtrelease.ReleasePlugin.autoImport._


  lazy val sbtAssemblySettings = assemblySettings ++ Seq(

    // Skip tests
    // test in assembly := {},

    fork := true,

    // Slightly cleaner jar name
    jarName in assembly := {
      name.value + "-" + version.value + ".jar"
    },

    // Drop these jars
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "jsp-api-2.1-6.1.14.jar",
        "jsp-2.1-6.1.14.jar",
        "jasper-compiler-5.5.12.jar",
        "commons-beanutils-core-1.8.0.jar",
        "commons-beanutils-1.7.0.jar",
        "servlet-api-2.5-20081211.jar",
        "servlet-api-2.5.jar"
      )
      cp filter { jar => excludes(jar.data.getName) }
    },

    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        // case "project.clj" => MergeStrategy.discard // Leiningen build files
        case x if x.startsWith("META-INF") => MergeStrategy.discard // Bumf
        case x if x.endsWith(".html") => MergeStrategy.discard // More bumf
        case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last // For Log$Logger.class
        case x => old(x)
      }
    }
  )
  lazy val basicSettings = Seq[Setting[_]](
    organization := "com.philrene",
    description := "Playground",
    scalaVersion := "2.10.5",
    scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
    parallelExecution := false,
    javaOptions += "-Xmx4G",
    publishTo := Some(Resolver.file("file", new File("/tmp/")))
  )

  lazy val forceReleaseVersion = scala.util.Properties.propOrNull("releaseVersion")
  println(forceReleaseVersion)

  lazy val releaseSettings = projectSettings ++ Seq[Setting[_]](
    releaseVersion := { if (forceReleaseVersion != null)
                          ver =>  forceReleaseVersion
                        else
                          ver => Version(ver).map(_.withoutQualifier.string).getOrElse(versionFormatError)},
    releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.asSnapshot.string).getOrElse(versionFormatError) }
  )

  lazy val buildSettings = basicSettings ++ sbtAssemblySettings ++ releaseSettings

  // Configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }


  // Define our project, with basic project information and library dependencies
  lazy val project = Project("scala-playground", file("."))
    .settings(buildSettings: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(
      artifact in(Compile, assembly) ~= { art =>
        art.copy(`classifier` = Some("assembly"))
      }
    )
    .settings(addArtifact(artifact in(Compile, assembly), assembly).settings: _*)

}

