package com.github.mikeneck.pkl.argo.workflows.core

import java.io.File

data class AliasDefinition(
    override val typeName: String,
    override val description: Comment,
    val alias: Type,
): Definition {

    override val isAlias: Boolean get() = true

    override val simpleName: String get() = typeName.split(".").last()

    override fun collectDependentReferences(debug: Boolean): Set<Type> = alias.collectDependentReferences(debug)

    override fun filepathFromSourceRoot(): String = typeName.split("/").last().replace(".", File.separator)

    override fun code(debug: Boolean): List<String> =
        listOf("typealias ${typeName.split(".").last()} = ${alias.kind.string}")

    val comments: List<String> get()= description.makeLines(simpleName)
}
