package ru.cs.roadcheck.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.cs.roadcheck.common.domain.entities.RiskPrediction
import java.math.BigDecimal
import java.time.Instant

interface RiskPredictionRepository : JpaRepository<RiskPrediction, Long> {

    @Query(
        value = """
            select avg(risk_score) 
            from public.risk_predictions 
            where calculated_at > :since
        """,
        nativeQuery = true
    )
    fun findAverageRiskScoreSince(@Param("since") since: Instant): BigDecimal?

    @Query(
        value = """
            select avg(risk_score) 
            from public.risk_predictions 
            where calculated_at > :since
              and region_name in (
                  select reg_name from public.regions where id in (:regionIds)
              )
        """,
        nativeQuery = true,
    )
    fun findAverageRiskScoreSinceForRegions(
        @Param("since") since: Instant,
        @Param("regionIds") regionIds: Collection<Long>,
    ): BigDecimal?
}
