import org.jsonschema2pojo.SourceType

plugins {
    id("java")
    id("org.jsonschema2pojo") version("1.2.2")
    id("org.pkl-lang") version "0.27.0"
}

jsonSchema2Pojo {
    setSource(files("data"))
    setSourceType(SourceType.JSONSCHEMA.name)
    generateBuilders = true
    targetDirectory = file("build/generated-sources/openapi")
    targetPackage = "io.argoproj.workflows"
}
