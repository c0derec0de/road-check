package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "risk_predictions", schema = "public")
class RiskPrediction : BaseEntity() {

    @ManyToOne
    @JoinColumn(
        name = "zone_id",
        foreignKey = ForeignKey(name = "risk_predictions_zone_id_fkey"),
    )
    var zone: DangerousZone? = null

    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    var latitude: BigDecimal? = null

    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    var longitude: BigDecimal? = null

    @Column(name = "region_name", length = 100)
    var regionName: String? = null

    @Column(name = "city_name", length = 100)
    var cityName: String? = null

    @Column(name = "district_name", length = 100)
    var districtName: String? = null

    @Column(name = "risk_level", length = 10, nullable = false)
    var riskLevel: String? = null

    @Column(name = "risk_score", precision = 5, scale = 2)
    var riskScore: BigDecimal? = null

    @Column(name = "probability_high", precision = 5, scale = 4)
    var probabilityHigh: BigDecimal? = null

    @Column(name = "probability_medium", precision = 5, scale = 4)
    var probabilityMedium: BigDecimal? = null

    @Column(name = "probability_low", precision = 5, scale = 4)
    var probabilityLow: BigDecimal? = null

    @Column(name = "predicted_incidents_next_7d")
    var predictedIncidentsNext7d: Int? = null

    @Column(name = "predicted_incidents_next_30d")
    var predictedIncidentsNext30d: Int? = null

    @Column(name = "confidence_level", precision = 5, scale = 4)
    var confidenceLevel: BigDecimal? = null

    @Column(name = "model_version", length = 50, nullable = false)
    var modelVersion: String? = null

    @Column(name = "model_accuracy", precision = 5, scale = 4)
    var modelAccuracy: BigDecimal? = null

    @Column(name = "calculated_at")
    var calculatedAt: Instant? = null

    @Column(name = "forecast_date")
    var forecastDate: LocalDate? = null

    @Column(name = "forecast_end_date")
    var forecastEndDate: LocalDate? = null

    @Column(name = "is_active")
    var isActive: Boolean? = true

    @Column(name = "contributing_factors", columnDefinition = "jsonb")
    var contributingFactors: String? = null

    @Column(name = "weather_contribution", precision = 5, scale = 4)
    var weatherContribution: BigDecimal? = null

    @Column(name = "historical_contribution", precision = 5, scale = 4)
    var historicalContribution: BigDecimal? = null

    @Column(name = "seasonal_contribution", precision = 5, scale = 4)
    var seasonalContribution: BigDecimal? = null

    @Column(name = "validation_method", length = 50)
    var validationMethod: String? = null

    @Column(name = "validation_score", precision = 5, scale = 4)
    var validationScore: BigDecimal? = null
}
