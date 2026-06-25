package ru.cs.roadcheck.rest.dto.manager

import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.domain.entities.User
import ru.cs.roadcheck.common.domain.entities.UserRole

@Schema(description = "Запрос создания/обновления пользователя")
data class UserManagerRequest(
    val login: String?,
    val vkId: String?,
    val firstname: String?,
    val middlename: String?,
    val lastname: String?,
    val department: String?,
    val city: String?,
    val phone: String?,
    val email: String?,
    val walletAddress: String?,
)

@Schema(description = "Ответ пользователя")
data class UserManagerResponse(
    val id: Long,
    val role: String,
    val login: String?,
    val vkId: String?,
    val firstname: String?,
    val middlename: String?,
    val lastname: String?,
    val department: String?,
    val city: String?,
    val phone: String?,
    val email: String?,
    val walletAddress: String?,
    val blockchainVerified: Boolean?,
)

fun User.toUserManagerResponse() = UserManagerResponse(
    id = id!!,
    role = role.name,
    login = login,
    vkId = vkId,
    firstname = firstname,
    middlename = middlename,
    lastname = lastname,
    department = department,
    city = city,
    phone = phone,
    email = email,
    walletAddress = walletAddress,
    blockchainVerified = blockchainVerified ?: false,
)

fun UserManagerRequest.toUser(): User {
    val req = this
    return User().apply {
        login = req.login
        vkId = req.vkId
        firstname = req.firstname
        middlename = req.middlename
        lastname = req.lastname
        department = req.department
        city = req.city
        phone = req.phone
        email = req.email
        walletAddress = req.walletAddress
        role = UserRole.USER
    }
}
