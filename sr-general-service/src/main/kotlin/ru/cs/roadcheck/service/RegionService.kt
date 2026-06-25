package ru.cs.roadcheck.service

import org.springframework.stereotype.Service
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.RegionRepository
import ru.cs.roadcheck.rest.dto.RegionResponse
import ru.cs.roadcheck.rest.dto.toRegionResponse

@Service
class RegionService(
    private val regionRepository: RegionRepository,
) {

    fun findAll(): List<RegionResponse> =
        regionRepository.findAll().map { it.toRegionResponse() }

    fun findById(id: Long): RegionResponse =
        regionRepository.findById(id)
            .orElseThrow { NotFoundException("Регион с id=$id не найден") }
            .toRegionResponse()
}
