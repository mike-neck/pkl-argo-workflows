package com.github.mikeneck.pkl.argo.workflows

import com.github.mikeneck.pkl.argo.workflows.core.Comment
import com.github.mikeneck.pkl.argo.workflows.core.Property
import com.github.mikeneck.pkl.argo.workflows.core.Property.ExtractorByRegex
import com.github.mikeneck.pkl.argo.workflows.core.ScalarType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

typealias TestName = String

typealias ExpectedValue = String?

class PropertyTest {

  companion object {
    @JvmStatic infix fun <A, B, C> Pair<A, B>.too(c: C): Triple<A, B, C> = Triple(first, second, c)

    @JvmStatic
    fun providePropertyForGetDefaultValue(): Iterable<Triple<TestName, Property, ExpectedValue>> {
      return listOf(
          "kubebuilder format" to
                  Property("value", ScalarType.STRING, Comment(null, "+kubebuilder:default=test")) too
              "test",
          "kubebuilder without data" to
                  Property(
                      "value", ScalarType.STRING, Comment(null, "+kubebuilder:subresource:status")
                  ) too
              null,
          "Defaults to value" to
                  Property(
                      "value",
                      ScalarType.STRING,
                      Comment("value", "This is the value. Defaults to nothing.")
                  ) too
              "nothing",
          "Defaults to value" to
                  Property(
                      "value",
                      ScalarType.STRING,
                      Comment("value", "This is the value. Defaults to nothing. We provide 2 type of values. 'nothing' and 'testing'.")
                  ) too
                  "nothing",
          )
    }

    val Triple<TestName, Property, ExpectedValue>.testName: TestName
      get() = this.first

    val Triple<TestName, Property, ExpectedValue>.property: Property
      get() = this.second

    val Triple<TestName, Property, ExpectedValue>.expectedValue: ExpectedValue
      get() = this.third
  }

  @MethodSource("providePropertyForGetDefaultValue")
  @ParameterizedTest
  fun getDefaultValue(data: Triple<TestName, Property, String>) {
    val defaultValue = data.property.getDefaultValue()
    assertAll(
        data.testName,
        {
          assertEquals(data.expectedValue, defaultValue) {
            "${data.testName}: expected: ${data.expectedValue} real: $defaultValue"
          }
        },
    )
  }
}
