package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "dangerous_zones", schema = "public")
class DangerousZone : BaseEntity() {

    @Column(name = "name", columnDefinition = "text")
    var name: String? = null

    @Column(name = "center_lat", precision = 9, scale = 6)
    var centerLat: BigDecimal? = null

    @Column(name = "center_lng", precision = 9, scale = 6)
    var centerLng: BigDecimal? = null

    @Column(name = "radius")
    var radius: Int? = null

    @Column(name = "incidents_count")
    var incidentsCount: Int? = null

    @Column(name = "risk_level", length = 10)
    var riskLevel: String? = null

    @Column(name = "calculated_at")
    var calculatedAt: Instant? = null

    @Column(name = "is_active")
    var isActive: Boolean? = true

    @Column(name = "region_id")
    var regionId: Long? = null
}

