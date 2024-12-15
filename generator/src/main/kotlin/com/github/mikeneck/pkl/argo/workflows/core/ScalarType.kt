package com.github.mikeneck.pkl.argo.workflows.core

enum class ScalarType(
    override val typeName: String,
    override val kind: Kind = Kind.Scalar(typeName),
): Type {
    STRING("String"),
    INT("Int"),
    FLOAT("Float"),
    BOOLEAN("Boolean"),
    ANY("Any"),
    NULL("Null"),
    NUMBER("Number"),
    REGEX("Regex"),
    REGEX_MATCH("RegexMatch"),
    DURATION("Duration"),
    DATA_SIZE("DataSize"),
    NON_NULL("NonNull"),
    ;

    companion object {
        @JvmStatic
        operator fun invoke(typeName: String): ScalarType? = entries.find { it.typeName.equals(typeName, true) }
    }
}