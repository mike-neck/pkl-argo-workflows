package com.github.mikeneck.pkl.argo.workflows.pkl

import com.github.mikeneck.pkl.argo.workflows.App
import com.github.mikeneck.pkl.argo.workflows.core.Definitions
import com.github.mikeneck.pkl.argo.workflows.core.ModuleDefinition
import com.github.mikeneck.pkl.argo.workflows.json.CustomMappings
import com.github.mikeneck.pkl.argo.workflows.json.Jackson
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PklModuleTest {

  @Test
  fun test() {
    val logEntry = definitions.find { it.simpleName == "LogEntry" }
    assertNotNull(logEntry)
    val pklMod = PklModule(definitions.aliases, logEntry as ModuleDefinition)
    val aliasNames = definitions.aliases.map { it.typeName }
    val imports = pklMod.imports
    assertAll(imports) { it !in aliasNames }
  }

  companion object {
    fun <T> assertAll(collection: Iterable<T>, executable: (T) -> Unit) {
      val executables: List<() -> Unit> = collection.map { { executable(it) } }
      org.junit.jupiter.api.assertAll(executables)
    }

    val definitions: Definitions by lazy {
      val jackson = Jackson(CustomMappings(App.ITEM_AS_ALIAS))
      val file = findFile(App.DEFAULT_SCHEMA_PATH)
      if (file == null) {
        throw IllegalStateException("file ${App.DEFAULT_SCHEMA_PATH} not found")
      }
      jackson.read(file)
    }

    tailrec fun findFile(path: String, dir: Path = Path("test-data").absolute().parent): Path? {
      return when {
        dir == dir.root -> null
        dir.resolve(path).exists() -> dir.resolve(path)
        else -> findFile(path, dir.parent)
      }
    }
  }
}
