package ru.cs.roadcheck.rest.dto.manager

import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.domain.entities.Road

@Schema(description = "Запрос создания/обновления дороги")
data class RoadManagerRequest(
    val roadName: String?,
)

@Schema(description = "Ответ дороги")
data class RoadManagerResponse(
    val id: Long,
    val roadName: String?,
)

fun Road.toRoadManagerResponse() = RoadManagerResponse(
    id = id!!,
    roadName = roadName,
)

fun RoadManagerRequest.toRoad(): Road {
    val req = this
    return Road().apply {
        roadName = req.roadName
    }
}
