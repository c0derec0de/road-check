package ru.cs.roadcheck.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.cs.roadcheck.common.domain.entities.Weather
import java.math.BigDecimal

interface WeatherRepository : JpaRepository<Weather, Long> {

    @Query(
        value = """
            select * 
            from public.weather 
            where latitude = :lat 
              and longitude = :lng 
            order by timestamp desc
        """,
        nativeQuery = true
    )
    fun findLatestByCoordinates(@Param("lat") lat: BigDecimal, @Param("lng") lng: BigDecimal): List<Weather>
}
