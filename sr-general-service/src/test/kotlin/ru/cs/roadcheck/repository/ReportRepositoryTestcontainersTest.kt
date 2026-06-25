package ru.cs.roadcheck.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import java.time.Instant

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportRepositoryTestcontainersTest(
    @Autowired private val reportRepository: ReportRepository,
) {

    @Test
    fun `archiveProcessedReports archives only processed statuses`() {
        reportRepository.save(newReport(status = ReportStatus.CONFIRMED))
        reportRepository.save(newReport(status = ReportStatus.DECLINED))
        reportRepository.save(newReport(status = ReportStatus.NEW))

        val archived = reportRepository.archiveProcessedReports()

        assertThat(archived).isEqualTo(2)
        assertThat(reportRepository.findAllByStatusNot(ReportStatus.ARCHIEVED))
            .hasSize(1)
            .allMatch { it.status == ReportStatus.NEW }
    }

    @Test
    fun `findByStatusAndRiskLevel excludes archived reports`() {
        reportRepository.save(newReport(status = ReportStatus.NEW, riskLevel = "high"))
        reportRepository.save(newReport(status = ReportStatus.NEW, riskLevel = "high"))
        reportRepository.save(newReport(status = ReportStatus.ARCHIEVED, riskLevel = "high"))

        val result = reportRepository.findByStatusAndRiskLevel(
            status = "NEW",
            riskLevel = "high",
            regionId = null,
            userId = null,
            pageable = org.springframework.data.domain.PageRequest.of(0, 10),
        )

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content).allMatch { it.status == ReportStatus.NEW }
    }

    private fun newReport(status: ReportStatus, riskLevel: String? = null): Report {
        val now = Instant.now()
        return Report().apply {
            policeUserId = 1L
            userId = 1L
            incidentType = "ДТП"
            description = "Тестовый инцидент"
            this.status = status
            this.riskLevel = riskLevel
            createdAt = now
            updatedAt = now
        }
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("roadcheck_test")
            .withUsername("roadcheck")
            .withPassword("roadcheck")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.liquibase.enabled") { false }
        }
    }
}
