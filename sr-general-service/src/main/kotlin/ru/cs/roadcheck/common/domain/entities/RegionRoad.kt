package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "regions_roads", schema = "public")
@IdClass(RegionRoadId::class)
class RegionRoad {

    @Id
    @Column(name = "region_id", nullable = false)
    var regionId: Long? = null

    @Id
    @Column(name = "road_id", nullable = false)
    var roadId: Long? = null
}

data class RegionRoadId(
    var regionId: Long? = null,
    var roadId: Long? = null
) : Serializable
