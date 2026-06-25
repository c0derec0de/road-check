package ru.cs.roadcheck.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.RegionRepository
import ru.cs.roadcheck.rest.dto.RegionWeatherResponse

@Service
class RegionWeatherService(
    private val regionRepository: RegionRepository,
    private val externalWeatherClient: ExternalWeatherClient,
) {

    @Cacheable("regionWeatherNow")
    fun getNow(regionId: Long): RegionWeatherResponse {
        val region = regionRepository.findById(regionId)
            .orElseThrow { NotFoundException("Регион с id=$regionId не найден") }

        val lat = region.centerLat
            ?: throw NotFoundException("У региона с id=$regionId не заполнена широта центра")
        val lng = region.centerLng
            ?: throw NotFoundException("У региона с id=$regionId не заполнена долгота центра")

        val external = externalWeatherClient.getNow(lat, lng)
            ?: throw NotFoundException("Не удалось получить погоду для региона с id=$regionId")

        return RegionWeatherResponse(
            regionId = region.id!!,
            regionName = region.regName,
            temperature = external.temperature?.toInt(),
            precipitationMm = external.precipitation,
            windSpeedKmh = external.windSpeedKmh,
            humidity = external.humidity,
        )
    }
}