package com.github.mikeneck.pkl.argo.workflows.core

import java.io.File

sealed interface Definition {
    val typeName: String
    val isAlias: Boolean
    val simpleName: String
    val description: Comment
    fun collectDependentReferences(debug: Boolean): Set<Type>
    fun filepathFromSourceRoot(): String
    fun directoryFromSourceRoot(): String = filepathFromSourceRoot()
        .split(File.separator)
        .let { it[0..(it.lastIndex - 1)].joinToString(File.separator) }
    fun code(debug: Boolean = false): List<String>
}