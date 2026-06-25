package ru.cs.roadcheck.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.rest.dto.manager.DangerousZoneManagerRequest
import ru.cs.roadcheck.rest.dto.manager.toDangerousZone
import ru.cs.roadcheck.rest.dto.manager.toDangerousZoneManagerResponse

@Service
class DangerousZoneManagerService(private val dangerousZoneRepository: DangerousZoneRepository) {

    fun findAll() = dangerousZoneRepository.findByIsActiveTrue().map { it.toDangerousZoneManagerResponse() }

    fun findById(id: Long) = dangerousZoneRepository.findById(id)
        .orElseThrow { NotFoundException("Опасная зона с id=$id не найдена") }
        .toDangerousZoneManagerResponse()

    @Transactional
    fun create(request: DangerousZoneManagerRequest) = request.toDangerousZone().let {
        dangerousZoneRepository.save(it).toDangerousZoneManagerResponse()
    }

    @Transactional
    fun update(id: Long, request: DangerousZoneManagerRequest) = dangerousZoneRepository.findById(id)
        .orElseThrow { NotFoundException("Опасная зона с id=$id не найдена") }
        .apply {
            name = request.name
            centerLat = request.centerLat
            centerLng = request.centerLng
            radius = request.radius
            incidentsCount = request.incidentsCount
            riskLevel = request.riskLevel
            isActive = request.isActive ?: true
            regionId = request.regionId
        }
        .let { dangerousZoneRepository.save(it).toDangerousZoneManagerResponse() }

    @Transactional
    fun delete(id: Long) {
        if (!dangerousZoneRepository.existsById(id)) throw NotFoundException("Опасная зона с id=$id не найдена")
        dangerousZoneRepository.deleteById(id)
    }
}
