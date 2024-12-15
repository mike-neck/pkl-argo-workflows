package com.github.mikeneck.pkl.argo.workflows.pkl

import com.github.mikeneck.pkl.argo.workflows.core.AliasDefinition
import com.github.mikeneck.pkl.argo.workflows.core.Aliases
import com.github.mikeneck.pkl.argo.workflows.core.Kind
import com.github.mikeneck.pkl.argo.workflows.core.ModuleDefinition
import com.github.mikeneck.pkl.argo.workflows.core.collectDependentReferences

data class PklModule(
    val aliases: Aliases,
    val definition: ModuleDefinition,
) {

  val comments: List<String>
    get() = definition.description.makeLines(definition.simpleName)

  val typeName: String
    get() = definition.typeName

  fun Kind.isAliases(): Boolean {
    val typeName = this.string
    return aliases.any { it.typeName == typeName }
  }

  fun Kind.isNotAliases(): Boolean = !isAliases()

  val properties: List<PklProperty>
    get() = definition.properties.map { PklProperty(it) }

  val imports: List<String>
    get() {
      val propertyTypes = definition.properties.map { it.type }.filter { it.kind.isNotAliases() }
      val refs = ArrayDeque(propertyTypes).collectDependentReferences(false)
      return refs.map { it.kind }.flatMap { it.dependencyNames }
    }

  val aliasesDef: List<AliasDefinition>
    get() {
      val names = definition.properties.map { it.type.kind.string }
      return aliases.filter { it.typeName in names }
    }
}
