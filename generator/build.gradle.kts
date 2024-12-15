@file:Suppress("UnstableApiUsage")

import com.github.mikeneck.jte.JteTask

group = "com.github.mikeneck.pkl"

version = providers.environmentVariable("PKL_ARGO_VERSION").orElse("0.0.0-SNAPSHOT").get()

plugins {
  alias(libs.plugins.kotlin.jvm)
  application
  id("org.pkl-lang") version "0.27.0"
}

repositories { mavenCentral() }

dependencies {
  implementation(platform(libs.jackson))
  implementation(libs.openApiGenerator)
  implementation(libs.jsonschema2pojo)
  implementation(libs.jacksonModuleKotlin)
  implementation(libs.java.template.engine)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) { useJUnitJupiter("5.10.3") }
  }
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

application { mainClass = "com.github.mikeneck.pkl.argo.workflows.App" }

val generatedPklDir = rootProject.layout.buildDirectory.dir("generated/pkl")
val pklProjectDir = generatedPklDir.map { it.dir("src/main/pkl") }
val pklArgoWorkflowsDir = rootProject.layout.buildDirectory.dir("pkl/argo-workflows/${project.version}")

tasks {
  val destinationDir = layout.buildDirectory.dir("generated/jte")
  val classesDir = layout.buildDirectory.dir("classes/jte")

  create<JteTask>("compileJte") {
    src.set(
        sourceSets.main.flatMap {
          layout.dir(project.provider { it.resources.sourceDirectories.first() })
        })
    classpath.from(configurations.compileClasspath)
    classpath.from(sourceSets.main.map { it.output })
    packageName.set("com.github.mikeneck.pkl")
    destination.set(destinationDir)
    dependsOn("processResources", "compileJava")
  }
  create<Copy>("copyJte") {
    dependsOn("compileJte")
    inputs.dir(destinationDir)
    outputs.dir(classesDir)
    from(destinationDir) { include("**/*.class") }
    into(classesDir)
  }
  create("jte") {
    dependsOn("compileJte", "copyJte")
    outputs.dir(destinationDir)
    outputs.dir(classesDir)
  }
  named<Jar>("jar") {
    dependsOn("jte")
    inputs.dir(classesDir)
    from(classesDir)
  }
  named<JavaExec>("run") {
    dependsOn("jte")
    val schemaDataFile = rootProject.layout.projectDirectory.dir("data").file("schema.json")
    inputs.dir(classesDir)
    inputs.file(schemaDataFile)
    inputs.property("project-version", project.version)
    classpath(classesDir)
    outputs.dir(generatedPklDir)
    args(
        schemaDataFile.asFile.absolutePath,
        generatedPklDir.get().asFile.absolutePath,
    )
    doLast {
      val projectFileDir = generatedPklDir.map { it.dir("src/main/pkl") }
      val resourcesSet: Provider<File> = sourceSets.main.flatMap { ss ->
        val f = ss.resources.find { it.name == "PklProject" }
        return@flatMap if (f != null) project.provider { f } else project.objects.fileProperty().asFile
      }
      copy {
        from(resourcesSet)
        into(projectFileDir)
        filter { line: String ->
          if (line.contains("0.0.0")) {
            line.replace("0.0.0", project.version.toString())
          } else {
            line
          }
        }
      }
    }
  }
  create("generate-pkl") {
    group = "build"
    description = "Generates pkl definitions for argo-workflows"
    dependsOn("run")
    inputs.files(sourceSets.main.map { it.resources })
    outputs.file(generatedPklDir)
  }
}

pkl {
  project {
    packagers {
      register("pkl-argo-workflows") {
        projectDirectories.from(pklProjectDir)
        outputPath.set(pklArgoWorkflowsDir)
        junitReportsDir = layout.buildDirectory.dir("reports/pkl")
        overwrite = true
      }
    }
  }
}

tasks.named("pkl-argo-workflows") {
  dependsOn("generate-pkl")
  group = "build"
}
