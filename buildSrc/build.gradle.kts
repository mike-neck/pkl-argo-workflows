plugins {
    id("org.jetbrains.kotlin.jvm") version("2.0.0")
}

repositories {
    mavenCentral()
}

dependencies {
    gradleApi()
    implementation("gg.jte:jte:3.1.14")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
