package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "reports", schema = "public")
class Report : BaseEntity() {

    @Column(name = "police_user_id", nullable = false)
    var policeUserId: Long? = null

    @Column(name = "user_id")
    var userId: Long? = null

    @Column(name = "incident_type", nullable = false, length = 100)
    var incidentType: String? = null

    @Column(name = "latitude", precision = 10, scale = 8)
    var latitude: BigDecimal? = null

    @Column(name = "longitude", precision = 11, scale = 8)
    var longitude: BigDecimal? = null

    @Column(name = "description")
    var description: String? = null

    @Column(name = "comment")
    var comment: String? = null

    @Column(name = "photos_uuid", length = 255)
    var photosUuid: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    var status: ReportStatus? = ReportStatus.NEW

    @Column(name = "blockchain_tx_hash", length = 255)
    var blockchainTxHash: String? = null

    @Column(name = "blockchain_verified")
    var blockchainVerified: Boolean? = false

    @Column(name = "blockchain_block_number")
    var blockchainBlockNumber: Long? = null

    @Column(name = "fatalities")
    var fatalities: Int? = 0

    @Column(name = "injuries")
    var injuries: Int? = 0

    @Column(name = "cause", length = 255)
    var cause: String? = null

    @Column(name = "risk_level", length = 10)
    var riskLevel: String? = null

    @Column(name = "title", length = 255)
    var title: String? = null

    @Column(name = "address", length = 500)
    var address: String? = null

    @Column(name = "is_dangerous_zone")
    var isDangerousZone: Boolean? = false

    @Column(name = "region_id")
    var regionId: Long? = null
}
