plugins {
    alias(libs.plugins.kotlin.jvm)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    application
}

group = "io.r03el"
version = "0.0.1"

application {
    mainClass.set("io.r03el.photograbber.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {

    // --- Koin (БЕЗ koin-ktor) ---
    implementation("io.insert-koin:koin-core:${libs.versions.koin.get()}")
    implementation("io.insert-koin:koin-logger-slf4j:${libs.versions.koin.get()}")
    implementation("io.github.kotlin-telegram-bot:kotlin-telegram-bot:6.3.0") {
        exclude(group = "io.ktor")
    }
    implementation("org.slf4j:slf4j-simple:1.7.36")

    implementation("io.minio:minio:8.5.12")
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation(libs.kotlin.test.junit)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}
