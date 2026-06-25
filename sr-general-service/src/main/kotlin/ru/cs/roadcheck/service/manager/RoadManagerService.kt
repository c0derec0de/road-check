package ru.cs.roadcheck.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.RoadRepository
import ru.cs.roadcheck.rest.dto.manager.RoadManagerRequest
import ru.cs.roadcheck.rest.dto.manager.toRoad
import ru.cs.roadcheck.rest.dto.manager.toRoadManagerResponse

@Service
class RoadManagerService(private val roadRepository: RoadRepository) {

    fun findAll() = roadRepository.findAll().map { it.toRoadManagerResponse() }

    fun findById(id: Long) = roadRepository.findById(id)
        .orElseThrow { NotFoundException("Дорога с id=$id не найдена") }
        .toRoadManagerResponse()

    @Transactional
    fun create(request: RoadManagerRequest) = request.toRoad().let {
        roadRepository.save(it).toRoadManagerResponse()
    }

    @Transactional
    fun update(id: Long, request: RoadManagerRequest) = roadRepository.findById(id)
        .orElseThrow { NotFoundException("Дорога с id=$id не найдена") }
        .apply { roadName = request.roadName }
        .let { roadRepository.save(it).toRoadManagerResponse() }

    @Transactional
    fun delete(id: Long) {
        if (!roadRepository.existsById(id)) throw NotFoundException("Дорога с id=$id не найдена")
        roadRepository.deleteById(id)
    }
}
