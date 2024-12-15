package com.github.mikeneck.pkl.argo.workflows.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.mikeneck.pkl.argo.workflows.core.AliasDefinition
import com.github.mikeneck.pkl.argo.workflows.core.ArrayType
import com.github.mikeneck.pkl.argo.workflows.core.Comment
import com.github.mikeneck.pkl.argo.workflows.core.Definition
import com.github.mikeneck.pkl.argo.workflows.core.Definitions
import com.github.mikeneck.pkl.argo.workflows.core.MapType
import com.github.mikeneck.pkl.argo.workflows.core.ModuleDefinition
import com.github.mikeneck.pkl.argo.workflows.core.ObjectType
import com.github.mikeneck.pkl.argo.workflows.core.Type
import com.github.mikeneck.pkl.argo.workflows.core.Property
import com.github.mikeneck.pkl.argo.workflows.core.RootDefinition
import com.github.mikeneck.pkl.argo.workflows.core.ScalarType
import java.nio.file.Path
import kotlin.collections.contains
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

class Jackson(
    val mappings: CustomMappings = CustomMappings(emptySet()),
    private val mapper: ObjectMapper =
        ObjectMapper()
            .registerModules(
                KotlinModule.Builder().enable(KotlinFeature.NullIsSameAsDefault).build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
) {

  inline fun <reified T : Any> ObjectMapper.readValue(file: Path, type: KClass<T> = T::class): T {
    return this.readValue(file.toFile(), type.java)
  }

  fun Body.mapToDefinition(name: String): Definition {
    return when {
      (name to this) in mappings -> mappings[(name to this)]
      type.isScalar() -> AliasDefinition(name, comment, type.asScalarType())
      ref.isAvailable() -> AliasDefinition(name, comment, ObjectType(ref))
      ref.isUnavailableObjectType() ->
          AliasDefinition(name, comment, MapType(ScalarType.STRING, ScalarType.STRING))

      items.isAvailable() -> AliasDefinition(name, comment, ArrayType(items.asType))
      else -> properties.mapToModuleDefinition(name, comment)
    }
  }

  fun RawProperties.mapToModuleDefinition(name: String, comment: Comment): Definition {
      val props = withDefault(emptyMap()).map { (key, prop) ->
          val pklType =
              try {
                  prop.asType
              } catch (e: IllegalArgumentException) {
                  throw IllegalArgumentException("failed to map to property, @$name of $key, $prop", e)
              }
          Property(key, pklType, Comment(prop.title, prop.description))
      }
      return ModuleDefinition(name, comment, props)
  }

  fun read(file: Path): Definitions {
    val schema = mapper.readValue<Schema>(file)
    val rootModuleNames = schema.oneOf.map { it.module }
    val definitions =
        schema.definitions.map { (name, body) ->
          body.run {
            val definition = mapToDefinition(name)
            if (rootModuleNames.contains(name)) {
              RootDefinition(definition)
            } else {
              definition
            }
          }
        }
    return Definitions(definitions)
  }
}

data class Schema(
    @JsonProperty("\$id") val id: String,
    @JsonProperty("\$schema") val schema: String,
    val definitions: Map<String, Body>,
    val oneOf: OneOf,
    val type: String,
)

typealias OneOf = List<Root>

data class Root(@JsonProperty("\$ref") val module: String)

typealias BodyType = String

typealias Title = String?

typealias Description = String?
typealias RawProperties = Map<String, Prop>?

data class Body(
    val type: BodyType?,
    val title: Title,
    val description: Description,
    val properties: RawProperties,
    val items: ArrayElement,
    @JsonProperty("\$ref") val ref: TypeReference,
) {

  val comment: Comment
    get() = Comment(title, description)

  fun BodyType.asScalarType(): Type =
      when (this) {
        "string" -> ScalarType.STRING
        "integer" -> ScalarType.INT
        "number" -> ScalarType.NUMBER
        "boolean" -> ScalarType.BOOLEAN
        else -> throw IllegalArgumentException("Unsupported type $this")
      }

  @OptIn(ExperimentalContracts::class)
  fun BodyType?.isScalar(): Boolean {
    contract { returns(true) implies (this@isScalar != null) }
    return this in listOf("string", "integer", "number", "boolean")
  }

  @OptIn(ExperimentalContracts::class)
  fun TypeReference.isAvailable(): Boolean {
    contract { returns(true) implies (this@isAvailable != null) }
    return type == "object" && this != null
  }

  @OptIn(ExperimentalContracts::class)
  fun TypeReference.isUnavailableObjectType(): Boolean {
    contract { returns(true) implies (this@isUnavailableObjectType == null) }
    return type == "object" && this == null && properties.isNullOrEmpty()
  }

  @OptIn(ExperimentalContracts::class)
  fun ArrayElement.isAvailable(): Boolean {
    contract { returns(true) implies (this@isAvailable != null) }
    return type == "array" && this != null
  }

  val isAlias: Boolean
    get() =
        when {
          type in listOf("string", "integer", "number") -> true
          type == "array" && items != null -> true
          type == "object" && ref != null -> true
          else -> false
        }
}

fun <T> T?.withDefault(dv: T): T = this ?: dv

typealias Scalar = String?

typealias ArrayElement = Prop?

typealias MapValueElement = Prop?

typealias TypeReference = String?

fun TypeReference.pklType(): Type =
    this?.let { ObjectType(it) }
        ?: throw IllegalStateException("null cannot be converted to pklType")

data class Prop(
    val type: Scalar,
    val description: Description,
    @JsonProperty("\$ref") val ref: TypeReference,
    val items: ArrayElement,
    val title: Title,
    val additionalProperties: MapValueElement,
) {
  val asType: Type
    get() =
        when {
          type.safe() -> type.scalarType()
          type.array(items) -> ArrayType(items.asType)
          type.obj(ref) -> ObjectType(ref)
          type.map(additionalProperties) ->
              MapType(ScalarType.STRING, additionalProperties.asType)
          type.array(items) -> ArrayType(items.asType)
          ref != null -> ObjectType(ref)
          type.explicitNull() -> ScalarType.NULL
          else -> throw IllegalArgumentException("unsupported type: $type")
        }
}

fun Scalar.scalarType(): Type =
    when (this) {
      "string" -> ScalarType.STRING
      "integer" -> ScalarType.INT
      "number" -> ScalarType.FLOAT
      "boolean" -> ScalarType.BOOLEAN
      else -> throw IllegalArgumentException("unsupported type: $this")
    }

@OptIn(ExperimentalContracts::class)
fun Scalar.safe(): Boolean {
  contract { returns(true) implies (this@safe != null) }
  if (this == null) {
    return false
  }
  return when (this) {
    "null",
    "object",
    "array" -> false
    else -> true
  }
}

@OptIn(ExperimentalContracts::class)
fun Scalar.explicitNull(): Boolean {
  contract { returns(true) implies (this@explicitNull != null) }
  if (this == null) {
    return false
  }
  return this == "null"
}

@OptIn(ExperimentalContracts::class)
fun Scalar.array(item: ArrayElement): Boolean {
  contract { returns(true) implies (item != null) }
  return this == "array" && item != null
}

@OptIn(ExperimentalContracts::class)
fun Scalar.obj(ref: TypeReference): Boolean {
  contract { returns(true) implies (ref != null) }
  return this == "object" && ref != null
}

@OptIn(ExperimentalContracts::class)
fun Scalar.map(additionalProperties: MapValueElement): Boolean {
  contract { returns(true) implies (additionalProperties != null) }
  return this == "object" && additionalProperties != null
}
