pluginManagement {
    val kotlinVersion = "2.2.21"
    val detektVersion = "1.23.8"

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion

        id("org.springframework.boot") version "3.1.5"
        id("io.spring.dependency-management") version "1.1.7"

        id("io.gitlab.arturbosch.detekt") version detektVersion
    }
}

rootProject.name = "roadcheck"
