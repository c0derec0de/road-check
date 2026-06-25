package ru.cs.roadcheck

import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.management.ManagementFactory
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

@Import(
	ru.cs.roadcheck.common.config.JwtConfig::class,
)
@EnableAutoConfiguration(
	exclude = [
		BatchAutoConfiguration::class,
		BeansEndpointAutoConfiguration::class,
	]
)
open class MainConfiguration

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableScheduling
@EnableJpaAuditing
@Import(
	MainConfiguration::class,
)
class RoadCheckApplication

fun main(args: Array<String>) {
	logger.info {
		"Starting application with VM options ${ManagementFactory.getRuntimeMXBean().inputArguments} " +
				"and CLI options ${args.contentToString()}"
	}
	runApplication<RoadCheckApplication>(*args)
}
