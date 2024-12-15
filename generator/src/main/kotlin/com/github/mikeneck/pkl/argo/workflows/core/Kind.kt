package com.github.mikeneck.pkl.argo.workflows.core

import com.github.mikeneck.pkl.argo.workflows.core.ScalarType.Companion.invoke

sealed interface Kind {
    val string: String
    val simple: String get() = string.split(".").last()
    val dependencyNames: List<String> get()= listOf("${string.replace('.', '/')}.pkl")
    companion object {
        operator fun invoke(string: String): Kind = ScalarType(string)?.kind ?: Types(string)
    }
    data class Scalar(override val string: String) : Kind {
        override val dependencyNames: List<String> get() = emptyList()
    }
    data class Types(private val typeRef: String) : Kind {
        override val string: String = typeRef.split("/").last()
    }
    data class UnaryTypes(val name: String, val element: Kind) : Kind {
        override val string: String = "$name<${element.simple}>"
        override val dependencyNames: List<String> get() = element.dependencyNames
    }

    data class BinaryTypes(val name: String, val fst: Kind, val snd: Kind) : Kind {
        override val string: String = "$name<${fst.string},${snd.simple}>"
        override val dependencyNames: List<String> get() = fst.dependencyNames + snd.dependencyNames
    }

    data class UnionTypes(val types: List<Kind>) : Kind {
        override val string: String get() = types.joinToString(separator = "|") { it.string }
        override val simple: String get() = types.joinToString(separator = "|") { it.simple }
        override val dependencyNames: List<String> get() = types.flatMap { it.dependencyNames }
    }
}