package ru.cs.roadcheck.common.domain.entities

import jakarta.persistence.*

@Entity
@Table(name = "users", schema = "public")
class User : BaseEntity() {

    @Column(name = "login", length = 255, nullable = false)
    var login: String? = null

    @Column(name = "vk_id", length = 255)
    var vkId: String? = null

    @Column(name = "firstname", length = 100)
    var firstname: String? = null

    @Column(name = "middlename", length = 100)
    var middlename: String? = null

    @Column(name = "lastname", length = 100)
    var lastname: String? = null

    @Column(name = "department", length = 150)
    var department: String? = null

    @Column(name = "city", length = 100)
    var city: String? = null

    @Column(name = "phone", length = 25)
    var phone: String? = null

    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null

    @Column(name = "wallet_address", length = 255)
    var walletAddress: String? = null

    @Column(name = "blockchain_tx_hash", length = 255)
    var blockchainTxHash: String? = null

    @Column(name = "blockchain_verified")
    var blockchainVerified: Boolean? = false

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    var role: UserRole = UserRole.USER
}
