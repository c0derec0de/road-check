plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "2.0.21"
	kotlin("plugin.jpa") version "2.0.21"

	id("org.springframework.boot") version "3.1.5"
	id("io.spring.dependency-management") version "1.1.7"

	id("io.gitlab.arturbosch.detekt") version "1.23.8"
	jacoco
	idea
}

group = "dev.roadcheck"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
		freeCompilerArgs.add("-java-parameters")
	}
}

repositories {
	mavenCentral()
	gradlePluginPortal()
}

apply(from = "dependencies.gradle")

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

sourceSets {
	create("integTest") {
		compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
		runtimeClasspath += output + sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
		java.srcDir("src/integTest/java")
		resources.srcDir("src/integTest/resources")
	}
}

configurations {
	getByName("integTestImplementation").extendsFrom(configurations.testImplementation.get())
	getByName("integTestRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
	config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
	reports {
		html.required.set(true)
		xml.required.set(false)
		txt.required.set(false)
	}
}

tasks.detekt {
	dependsOn(tasks.compileKotlin)
}

tasks.withType<Test> {
	useJUnitPlatform()
	maxHeapSize = "2g"
	systemProperty("file.encoding", "UTF-8")
	systemProperty("user.timezone", "UTC")

	testLogging {
		events("passed", "failed", "skipped", "started")
		showStandardStreams = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

		showExceptions = true
		showCauses = true
		showStackTraces = true

		debug {
			events("started", "skipped", "failed", "passed")
		}

		info.events = debug.events
		info.exceptionFormat = debug.exceptionFormat
	}

	addTestListener(object : TestListener {
		override fun beforeSuite(suite: TestDescriptor) {}
		override fun beforeTest(testDescriptor: TestDescriptor) {}
		override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
		override fun afterSuite(suite: TestDescriptor, result: TestResult) {
			if (suite.parent == null) {
				println("\nРезультаты тестирования: ${result.resultType}")
				println("Всего тестов: ${result.testCount}, " +
						"Успешно: ${result.successfulTestCount}, " +
						"Провалено: ${result.failedTestCount}, " +
						"Пропущено: ${result.skippedTestCount}")
			}
		}
	})
}

val test by tasks.getting(Test::class) {
	systemProperty("spring.profiles.active", "test,testcontainers")
	exclude("**/*IntegTest*")
	finalizedBy(tasks.jacocoTestReport)
}

val integTest by tasks.registering(Test::class) {
	testClassesDirs = sourceSets["integTest"].output.classesDirs
	classpath = sourceSets["integTest"].runtimeClasspath
	systemProperty("spring.profiles.active", "test,testcontainers")
	include("**/*IntegTest*")
	shouldRunAfter(test)
}

tasks.jacocoTestReport {
	dependsOn(test)

	reports {
		xml.required.set(true)
		html.required.set(true)
	}

	classDirectories.setFrom(
		sourceSets.main.get().output.asFileTree.matching {
			exclude(
				"**/dto/**",
				"**/entity/**",
				"**/config/**",
				"**/*Application*",
				"**/generated/**",
				"**/*blockchain*",
				"**/*manager*",
				"**/*Documentation*",
			)
		}
	)
}

tasks.bootJar {
	archiveFileName.set("roadcheck.jar")
	mainClass.set("ru.cs.roadcheck.RoadCheckApplicationKt")
	layered { enabled = true }
}

idea {
	module {
		testSources.from(file("src/integTest/java"))
	}
}

val localBootRun by tasks.registering {
	group = "application"

	doFirst {
		exec {
			val composeFile = "src/main/resources/docker-compose-local.yml"
			commandLine("docker-compose", "-f", composeFile, "-p", "roadcheck-general", "up", "-d")
		}
	}

	finalizedBy(tasks.bootRun)
}

tasks.bootRun {
	val isLocalBootRun = gradle.startParameter.taskNames.contains("localBootRun")
	val envFileName = if (isLocalBootRun) ".env" else ".env.production"
	val envFile = rootProject.file(envFileName)
	if (envFile.exists()) {
		val envFromFile = mutableMapOf<String, String>()
		envFile.forEachLine { rawLine ->
			val line = rawLine.trim()
			if (line.isEmpty() || line.startsWith("#")) return@forEachLine
			val delimiterIndex = line.indexOf('=')
			if (delimiterIndex <= 0) return@forEachLine
			val key = line.substring(0, delimiterIndex).trim()
			val value = line.substring(delimiterIndex + 1).trim()
			if (key.isNotEmpty()) {
				envFromFile[key] = value
			}
		}
		environment(envFromFile)
	}

	if (isLocalBootRun) {
		systemProperty("spring.profiles.active", "local")
	} else if (System.getProperty("spring.profiles.active").isNullOrBlank()) {
		systemProperty("spring.profiles.active", "prod")
	}
}

defaultTasks("build")
