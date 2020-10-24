import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    application
}
group = "com.scylladb.tracing.zipkin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("io.zipkin.brave:brave:5.12.7")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3:2.15.2")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("com.datastax.cassandra:cassandra-driver-core:3.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClassName = "MainKt"
}