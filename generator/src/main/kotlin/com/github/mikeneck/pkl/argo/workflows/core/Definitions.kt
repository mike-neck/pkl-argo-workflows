package com.github.mikeneck.pkl.argo.workflows.core

fun <T: Any> flatListOf(vararg lists: List<T>): List<T> = lists.asSequence().flatMap { it }.toList()

operator fun <T: Any> List<T>.get(indexRange: IntRange): List<T> {
    if (indexRange.last < 0 || this.lastIndex < indexRange.first) {
        throw IndexOutOfBoundsException("$indexRange out of range of this list")
    }
    return indexRange.map { this[it] }
}

typealias Aliases = List<AliasDefinition>

data class Definitions(
    val definitions:List<Definition>,
    val aliases: Aliases,
): Iterable<Definition> by definitions {

    companion object {
        operator fun invoke(definitions: List<Definition>): Definitions {
            val aliases = definitions.filterIsInstance<AliasDefinition>()
            return Definitions(definitions, aliases)
        }
    }
}
