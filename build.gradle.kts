// Build by running `gradle buildFatJar`.

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
  kotlin("jvm") version "2.0.0-RC3"
  id("io.ktor.plugin") version "2.3.11"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0-RC3"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

group = "com.roughlyunderscore"
version = "1.0"

application {
  mainClass.set("com.roughlyunderscore.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
  mavenLocal()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  maven("https://oss.sonatype.org/content/repositories/snapshots")
  maven("https://oss.sonatype.org/content/repositories/central")
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm")
  implementation("io.ktor:ktor-server-host-common-jvm")
  implementation("io.ktor:ktor-server-status-pages-jvm")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
  implementation("io.ktor:ktor-serialization-gson:$ktor_version")
  implementation("org.mongodb:mongodb-driver-sync:5.0.0")
  implementation("io.ktor:ktor-server-netty-jvm")
  implementation("ch.qos.logback:logback-classic:$logback_version")

  implementation("com.google.guava:guava:33.2.1-jre")
  implementation("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")

  implementation("de.mkammerer:argon2-jvm:2.11")

  implementation("com.roughlyunderscore:ULib:1.0")
  implementation("com.roughlyunderscore:UnderscoreEnchantsAPI:2.2.0")

  testImplementation("io.ktor:ktor-server-tests-jvm")
  implementation("io.ktor:ktor-server-rate-limit:$ktor_version")
  implementation("commons-io:commons-io:2.15.1")
  implementation("com.google.code.gson:gson:2.10.1")
}

ktor {
  fatJar {
    archiveFileName = "uebackend.jar"
  }
}