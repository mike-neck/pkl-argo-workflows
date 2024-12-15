package com.github.mikeneck.pkl.argo.workflows.pkl

import com.github.mikeneck.pkl.argo.workflows.core.Property
import com.github.mikeneck.pkl.argo.workflows.core.ScalarType

data class PklProperty(
    val property: Property,
) {

  fun getDefinitionLine(): String = "$name: $type${assignValue.withPrefix(" = ").value}"

  val name: String
    get() = PklReserved.wrapIfReservedWords(property.name)

  val type: String
    get() =
        when (property.isRequired()) {
          true -> property.type.kind.simple
          false -> "${property.type.kind.simple}?"
        }

  val assignValue: AssignValue
    get() =
        when (val value = property.getDefaultValue()) {
          null -> AssignValue.Empty
          else ->
              when (property.type) {
                ScalarType.INT ->
                    if (value.matches(IS_INT)) AssignValue.RawValue(value)
                    else AssignValue.Comment(value)
                ScalarType.FLOAT ->
                    if (value.matches(IS_FLOAT)) AssignValue.RawValue(value)
                    else AssignValue.Comment(value)
                ScalarType.BOOLEAN ->
                    when {
                      value.contains("true") -> AssignValue.RawValue("true")
                      value.contains("false") -> AssignValue.RawValue("false")
                      else -> AssignValue.Comment(value)
                    }
                ScalarType.NUMBER ->
                    when {
                      value.matches(IS_INT) -> AssignValue.RawValue(value)
                      value.matches(IS_FLOAT) -> AssignValue.RawValue(value)
                      else -> AssignValue.Comment(value)
                    }
                ScalarType.NULL -> AssignValue.RawValue("null")
                ScalarType.STRING -> AssignValue.RawValue.wrapQuot(value)
                else -> AssignValue.Comment(value)
              }
        }

  val commentLines: List<String> get() = property.comment.makeLines(name)

  fun hasDefaultValue(): Boolean {
    return property.hasDefaultValue()
  }

  companion object {
    val IS_INT: Regex = Regex("^(0|(-?[1-9][0-9]*))$")
    val IS_FLOAT: Regex = Regex("^(0|(-?[1-9][0-9]*))(\\.0*[0-9]+)?$")
  }
}
