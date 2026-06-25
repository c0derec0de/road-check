package ru.cs.roadcheck.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.cs.roadcheck.common.domain.entities.DangerousZone
import java.time.Instant

interface DangerousZoneRepository : JpaRepository<DangerousZone, Long> {

    fun findByIsActiveTrue(): List<DangerousZone>

    @Query(
        value = """
            select * 
            from public.dangerous_zones 
            where is_active = true 
              and (:regionId is null or region_id = :regionId)
        """,
        nativeQuery = true
    )
    fun findActiveByRegion(@Param("regionId") regionId: Long?): List<DangerousZone>

    @Query(
        value = """
            select * 
            from public.dangerous_zones 
            where is_active = true 
              and region_id in (:regionIds)
        """,
        nativeQuery = true,
    )
    fun findActiveByRegionIdIn(@Param("regionIds") regionIds: Collection<Long>): List<DangerousZone>

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true 
              and region_id in (:regionIds)
        """,
        nativeQuery = true,
    )
    fun countActiveByRegionIdIn(@Param("regionIds") regionIds: Collection<Long>): Long

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true 
              and created_at > :since 
              and region_id in (:regionIds)
        """,
        nativeQuery = true,
    )
    fun countByCreatedAtAfterAndRegionIdIn(@Param("since") since: Instant, @Param("regionIds") regionIds: Collection<Long>): Long

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true 
              and created_at between :start and :end 
              and region_id in (:regionIds)
        """,
        nativeQuery = true,
    )
    fun countByCreatedAtBetweenAndRegionIdIn(
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        @Param("regionIds") regionIds: Collection<Long>,
    ): Long

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true
        """,
        nativeQuery = true,
    )
    fun countActive(): Long

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true 
              and created_at > :since
        """,
        nativeQuery = true,
    )
    fun countActiveCreatedAtAfter(@Param("since") since: Instant): Long

    @Query(
        value = """
            select count(*) 
            from public.dangerous_zones 
            where is_active = true 
              and created_at between :start and :end
        """,
        nativeQuery = true,
    )
    fun countActiveCreatedAtBetween(@Param("start") start: Instant, @Param("end") end: Instant): Long
}
