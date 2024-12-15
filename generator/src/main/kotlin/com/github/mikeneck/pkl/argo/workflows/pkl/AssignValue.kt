package com.github.mikeneck.pkl.argo.workflows.pkl

sealed interface AssignValue {
    val value: String
    fun withPrefix(prefix: String): AssignValue
    object Empty : AssignValue {
        override val value: String get() = ""
        override fun withPrefix(prefix: String): AssignValue = this
    }
    data class RawValue(override val value: String) : AssignValue {
        companion object {
            fun wrapQuot(value: String): RawValue {
                println("wrapQuot[$value]")
                return RawValue("\"${value}\"")
            }
        }
        override fun withPrefix(prefix: String): AssignValue = RawValue("$prefix$value")
    }
    data class Comment(private val rawValue: String) : AssignValue {
        override val value: String get() = " // $rawValue"
        override fun withPrefix(prefix: String): Comment = this
    }
}