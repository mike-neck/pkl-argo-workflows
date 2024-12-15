package com.github.mikeneck.pkl.argo.workflows.core

interface Type {
    val typeName: String
    val kind: Kind
}

fun Type.collectDependentReferences(debug: Boolean = false): Set<Type> =
    ArrayDeque(listOf(this)).collectDependentReferences(debug)

tailrec fun ArrayDeque<Type>.collectDependentReferences(debug: Boolean = false, items: Set<Type> = emptySet()): Set<Type> {
    if (this.isEmpty()) return items
    val first = this.removeFirst()
    if (debug){ println("[DEBUG] item: ${first.typeName}, queue: ${this.size}") }
    return when (first) {
        is ScalarType -> collectDependentReferences(debug, items)
        is ArrayType -> {
            addLast(first.contentType)
            collectDependentReferences(debug, items)
        }
        is MapType -> {
            addLast(first.contentType)
            collectDependentReferences(debug, items)
        }
        is ObjectType -> {
            collectDependentReferences(debug, items + first)
        }
        else -> throw IllegalStateException("invalid pkl type ${first.typeName}()")
    }
}


data class ObjectType(
    override val typeName: String,
    override val kind: Kind = Kind(typeName),
): Type

data class ArrayType(
    val contentType: Type,
): Type {
    override val kind: Kind get() = Kind.UnaryTypes(typeName, contentType.kind)
    override val typeName: String get() = "Listing"
}

data class MapType(
    val keyType : Type,
    val contentType: Type,
): Type {
    override val kind: Kind get() = Kind.BinaryTypes(typeName, keyType.kind, contentType.kind)
    override val typeName: String get() = "Mapping"
}

data class UnionType(
    val types: List<Type>,
): Type {

    constructor(vararg types: Type) : this(types.toList())

    override val typeName: String get() = types.joinToString(separator = "|") { it.typeName }
    override val kind: Kind get() = Kind.UnionTypes(types.map { it.kind })
}
