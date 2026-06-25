package ru.cs.roadcheck.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import java.time.Instant

interface ReportRepository : JpaRepository<Report, Long> {

    @Query(
        value = """
            select * from public.reports
            where user_id = :userId
              and coalesce(status, '') <> 'ARCHIEVED'
            order by created_at desc
        """,
        nativeQuery = true,
    )
    fun findAllNonArchivedByUserId(@Param("userId") userId: Long): List<Report>

    fun findAllByStatusNot(status: ReportStatus): List<Report>

    fun findAllByUserId(userId: Long): List<Report>

    fun countByUserId(userId: Long): Long

    fun countByUserIdAndCreatedAtAfter(userId: Long, createdAt: Instant): Long

    fun countByUserIdAndCreatedAtBetween(userId: Long, start: Instant, end: Instant): Long

    fun countByUserIdAndStatus(userId: Long, status: ReportStatus): Long

    fun countByUserIdAndStatusAndCreatedAtAfter(userId: Long, status: ReportStatus, createdAt: Instant): Long

    fun countByUserIdAndStatusAndCreatedAtBetween(userId: Long, status: ReportStatus, start: Instant, end: Instant): Long

    @Query("select distinct r.regionId from Report r where r.userId = :userId and r.regionId is not null")
    fun findDistinctRegionIdsByUserId(@Param("userId") userId: Long): List<Long>

    @Query(
        value = """
            select count(*) 
            from public.reports 
            where created_at > :since
        """,
        nativeQuery = true
    )
    fun countByCreatedAtAfter(@Param("since") since: Instant): Long

    @Query(
        value = """
            select count(*) 
            from public.reports 
            where created_at between :start and :end
        """,
        nativeQuery = true
    )
    fun countByCreatedAtBetween(@Param("start") start: Instant, @Param("end") end: Instant): Long

    @Query(
        value = """
            select * 
            from public.reports 
            where (:status is null or status = :status) 
              and coalesce(status, '') <> 'ARCHIEVED'
              and (:riskLevel is null or risk_level = :riskLevel)
              and (:regionId is null or region_id = :regionId)
              and (:userId is null or user_id = :userId)
        """,
        countQuery = """
            select count(*) 
            from public.reports 
            where (:status is null or status = :status) 
              and coalesce(status, '') <> 'ARCHIEVED'
              and (:riskLevel is null or risk_level = :riskLevel)
              and (:regionId is null or region_id = :regionId)
              and (:userId is null or user_id = :userId)
        """,
        nativeQuery = true,
    )
    fun findByStatusAndRiskLevel(
        @Param("status") status: String?,
        @Param("riskLevel") riskLevel: String?,
        @Param("regionId") regionId: Long?,
        @Param("userId") userId: Long?,
        pageable: Pageable,
    ): Page<Report>

    @Query(
        value = """
            select count(*) 
            from public.reports 
            where status = :status
        """,
        nativeQuery = true
    )
    fun countByStatus(@Param("status") status: String): Long

    @Query(
        value = """
            select count(*) 
            from public.reports 
            where status = :status 
              and created_at > :since
        """,
        nativeQuery = true
    )
    fun countByStatusAndCreatedAtAfter(@Param("status") status: String, @Param("since") since: Instant): Long

    @Query(
        value = """
            select count(*) 
            from public.reports 
            where status = :status 
              and created_at between :start and :end
        """,
        nativeQuery = true
    )
    fun countByStatusAndCreatedAtBetween(@Param("status") status: String, @Param("start") start: Instant, @Param("end") end: Instant): Long

    @Query(
        value = """
            update public.reports
            set status = 'ARCHIEVED',
                updated_at = now()
            where status in ('CONFIRMED', 'DECLINED')
        """,
        nativeQuery = true
    )
    @org.springframework.data.jpa.repository.Modifying
    fun archiveProcessedReports(): Int
}
