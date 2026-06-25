package ru.cs.roadcheck.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.RegionRepository
import ru.cs.roadcheck.rest.dto.manager.RegionManagerRequest
import ru.cs.roadcheck.rest.dto.manager.toRegionManagerResponse
import ru.cs.roadcheck.rest.dto.manager.toRegion

@Service
class RegionManagerService(private val regionRepository: RegionRepository) {

    fun findAll() = regionRepository.findAll().map { it.toRegionManagerResponse() }

    fun findById(id: Long) = regionRepository.findById(id)
        .orElseThrow { NotFoundException("Регион с id=$id не найден") }
        .toRegionManagerResponse()

    @Transactional
    fun create(request: RegionManagerRequest) = run {
        if (request.regCode.isBlank()) throw ValidationException("regCode не может быть пустым")
        if (request.regName.isBlank()) throw ValidationException("regName не может быть пустым")
        request.toRegion().let { regionRepository.save(it).toRegionManagerResponse() }
    }

    @Transactional
    fun update(id: Long, request: RegionManagerRequest) = regionRepository.findById(id)
        .orElseThrow { NotFoundException("Регион с id=$id не найден") }
        .apply {
            regCode = request.regCode
            regName = request.regName
        }
        .let { regionRepository.save(it).toRegionManagerResponse() }

    @Transactional
    fun delete(id: Long) {
        if (!regionRepository.existsById(id)) throw NotFoundException("Регион с id=$id не найден")
        regionRepository.deleteById(id)
    }
}
