package com.github.mikeneck.pkl.argo.workflows

import com.github.mikeneck.pkl.argo.workflows.core.AliasDefinition
import com.github.mikeneck.pkl.argo.workflows.core.Aliases
import com.github.mikeneck.pkl.argo.workflows.core.Comment
import com.github.mikeneck.pkl.argo.workflows.core.Definition
import com.github.mikeneck.pkl.argo.workflows.core.Definitions
import com.github.mikeneck.pkl.argo.workflows.core.ModuleDefinition
import com.github.mikeneck.pkl.argo.workflows.core.RootDefinition
import com.github.mikeneck.pkl.argo.workflows.core.ScalarType
import com.github.mikeneck.pkl.argo.workflows.core.UnionType
import com.github.mikeneck.pkl.argo.workflows.json.CustomMappings
import com.github.mikeneck.pkl.argo.workflows.json.Jackson
import com.github.mikeneck.pkl.argo.workflows.json.Mapping
import com.github.mikeneck.pkl.argo.workflows.pkl.PklModule
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.FileOutput
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

object App {
  @JvmStatic val DEFAULT_SCHEMA_PATH = "data/schema.json"
  @JvmStatic val DEFAULT_OUTPUT_PATH = "build/pkl"
  @JvmStatic val DEFAULT_SOURCE_PATH = "src/main/pkl"

  @JvmStatic val JTE_PACKAGE_NAME = "com.github.mikeneck.pkl"

  @JvmStatic
  val ITEM_AS_ALIAS: Mapping =
      Mapping.ByName("io.argoproj.workflow.v1alpha1.Item") { name ->
        AliasDefinition(
            name,
            Comment(this.title, this.description),
            UnionType(ScalarType.STRING, ScalarType.BOOLEAN, ScalarType.INT, ScalarType.FLOAT))
      }

  @JvmStatic
  fun main(args: Array<String>) {
    val filepath = if (args.isEmpty()) DEFAULT_SCHEMA_PATH else args[0]
    val outputPath = if (args.size < 2) DEFAULT_OUTPUT_PATH else args[1]
    val sourcePath = if (args.size < 3) DEFAULT_SOURCE_PATH else args[2]
    println(filepath)
    println(outputPath)
    println(sourcePath)
    val definitions: Definitions = Jackson(CustomMappings(ITEM_AS_ALIAS)).read(Path(filepath))
    val destinationDir = Path.of(outputPath).resolve(sourcePath)
    val writer = TemplateWriter(destinationDir)
    println("=== === aliases === ===")
    definitions.aliases.forEach { println("${it.typeName} : ${ it.filepathFromSourceRoot() }") }
    println("=== === === ===")
    definitions.forEach { definition ->
      println("${ definition.simpleName }: ${ if (definition.isAlias) "[skip]" else "" }")
      if (definition.isAlias) return@forEach
      debugMod(definition, definitions.aliases)?.imports?.forEach { println("    - .../${it}") }
      try {
        writer.write(definitions.aliases, definition)
      } catch (e: Exception) {
        throw Exception("failed to write ${definition.simpleName}", e)
      }
    }
  }

  tailrec fun debugMod(definition: Definition, aliases: Aliases): PklModule? =
      when (definition) {
        is AliasDefinition -> null
        is ModuleDefinition -> PklModule(aliases, definition)
        is RootDefinition -> debugMod(definition.definition, aliases)
      }
}

class DefinitionWriter(val sourceRoot: Path) {

  fun write(definition: Definition) {
    val directoryFromSourceRoot = definition.directoryFromSourceRoot()
    val directory = sourceRoot.resolve(directoryFromSourceRoot)
    if (!(Files.exists(directory))) {
      Files.createDirectories(directory)
    }
    val file = directory.resolve("${definition.simpleName}.pkl")
    val lines = definition.code()
    Files.write(file, lines, Charsets.UTF_8)
  }
}

class TemplateWriter(
    val sourceRoot: Path,
    private val templateEngine: TemplateEngine =
        TemplateEngine.createPrecompiled(null, ContentType.Plain, null, App.JTE_PACKAGE_NAME),
) {

  fun write(aliases: Aliases, definition: Definition) {
    val directoryFromSourceRoot = definition.directoryFromSourceRoot()
    val directory = sourceRoot.resolve(directoryFromSourceRoot)
    if (!(Files.exists(directory))) {
      Files.createDirectories(directory)
    }
    val file = directory.resolve("${definition.simpleName}.pkl")
    FileOutput(file).use { it.writeRecursive(aliases, definition) }
  }

  private tailrec fun FileOutput.writeRecursive(aliases: Aliases, definition: Definition) {
    when (definition) {
      is AliasDefinition -> return
      is ModuleDefinition ->
          templateEngine.render("Module.jte", PklModule(aliases, definition), this)
      is RootDefinition -> this.writeRecursive(aliases, definition.definition)
    }
  }
}
