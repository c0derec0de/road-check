package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.domain.entities.Region
import java.math.BigDecimal

@Schema(description = "Регион")
data class RegionResponse(
    @Schema(description = "ID региона")
    val id: Long,
    @Schema(description = "Код региона")
    val regCode: String?,
    @Schema(description = "Название региона")
    val regName: String?,
    @Schema(description = "Широта центра региона")
    val centerLat: BigDecimal?,
    @Schema(description = "Долгота центра региона")
    val centerLng: BigDecimal?,
)

fun Region.toRegionResponse() = RegionResponse(
    id = id!!,
    regCode = regCode,
    regName = regName,
    centerLat = centerLat,
    centerLng = centerLng,
)
