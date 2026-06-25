package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*

@Entity
@Table(name = "roads", schema = "public")
class Road : BaseEntity() {

    @Column(name = "road_name", length = 255)
    var roadName: String? = null
}
