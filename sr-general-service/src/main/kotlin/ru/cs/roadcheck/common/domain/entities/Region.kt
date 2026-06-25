package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "regions", schema = "public")
class Region : BaseEntity() {

    @Column(name = "reg_code", nullable = false, length = 20)
    var regCode: String? = null

    @Column(name = "reg_name", nullable = false, length = 100)
    var regName: String? = null

    @Column(name = "center_lat", precision = 10, scale = 8)
    var centerLat: BigDecimal? = null

    @Column(name = "center_lng", precision = 11, scale = 8)
    var centerLng: BigDecimal? = null
}
