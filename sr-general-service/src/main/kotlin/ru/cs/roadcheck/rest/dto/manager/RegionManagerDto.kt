package ru.cs.roadcheck.rest.dto.manager

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import ru.cs.roadcheck.common.domain.entities.Region

@Schema(description = "Запрос создания/обновления региона")
data class RegionManagerRequest(
    val regCode: String,
    val regName: String,
    val centerLat: BigDecimal?,
    val centerLng: BigDecimal?,
)

@Schema(description = "Ответ региона")
data class RegionManagerResponse(
    val id: Long,
    val regCode: String?,
    val regName: String?,
    val centerLat: BigDecimal?,
    val centerLng: BigDecimal?,
)

fun Region.toRegionManagerResponse() = RegionManagerResponse(
    id = id!!,
    regCode = regCode,
    regName = regName,
    centerLat = centerLat,
    centerLng = centerLng,
)

fun RegionManagerRequest.toRegion(): Region {
    val req = this
    return Region().apply {
        regCode = req.regCode
        regName = req.regName
        centerLat = req.centerLat
        centerLng = req.centerLng
    }
}
