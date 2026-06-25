package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "weather", schema = "public")
class Weather : BaseEntity() {

    @Column(name = "timestamp", nullable = false)
    var timestamp: Instant? = null

    @Column(name = "humidity", precision = 5, scale = 2)
    var humidity: BigDecimal? = null

    @Column(name = "temperature", precision = 5, scale = 2)
    var temperature: BigDecimal? = null

    @Column(name = "wind_direction", length = 50)
    var windDirection: String? = null

    @Column(name = "wind_speed", precision = 6, scale = 2)
    var windSpeed: BigDecimal? = null

    @Column(name = "cloud_cover", precision = 5, scale = 2)
    var cloudCover: BigDecimal? = null

    @Column(name = "visibility", precision = 8, scale = 2)
    var visibility: BigDecimal? = null

    @Column(name = "dew_point", precision = 5, scale = 2)
    var dewPoint: BigDecimal? = null

    @Column(name = "precipitation", precision = 6, scale = 2)
    var precipitation: BigDecimal? = null

    @Column(name = "current_weather", length = 100)
    var currentWeather: String? = null

    @Column(name = "past_weather_1", length = 100)
    var pastWeather1: String? = null

    @Column(name = "past_weather_2", length = 100)
    var pastWeather2: String? = null

    @Column(name = "cloud_height", precision = 8, scale = 2)
    var cloudHeight: BigDecimal? = null

    @Column(name = "latitude", precision = 10, scale = 8)
    var latitude: BigDecimal? = null

    @Column(name = "longitude", precision = 11, scale = 8)
    var longitude: BigDecimal? = null
}
