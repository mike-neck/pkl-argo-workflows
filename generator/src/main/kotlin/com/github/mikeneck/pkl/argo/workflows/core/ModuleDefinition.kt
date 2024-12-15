package com.github.mikeneck.pkl.argo.workflows.core

import java.io.File

data class ModuleDefinition(
    override val typeName: String,
    override val description: Comment,
    val properties: List<Property>
): Definition {
    fun propertyLines(): List<String> = properties.map { "${it.name}: ${it.type.kind.string}" }

    override val isAlias: Boolean get() = false

    override val simpleName: String get() = typeName.split(".").last()

    override fun collectDependentReferences(debug: Boolean): Set<Type> =
        ArrayDeque(properties.map { it.type }).collectDependentReferences(debug)

    override fun filepathFromSourceRoot(): String =
        typeName.split("/").let { it[it.lastIndex] }.replace(".", File.separator)

    override fun code(debug: Boolean): List<String> = flatListOf(
        listOf("open module $typeName"),
        listOf(""),
        collectDependentReferences(debug).flatMap { it.kind.dependencyNames.map { "import \".../$it\"" } },
        listOf(""),
        properties.flatMap { listOf("${it.name}: ${it.type.kind.simple}", "") },
        listOf("")
    ).also { println("[DEBUG] module: $typeName") }
}