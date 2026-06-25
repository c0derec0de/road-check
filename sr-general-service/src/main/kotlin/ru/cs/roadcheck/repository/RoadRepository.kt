package ru.cs.roadcheck.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.cs.roadcheck.common.domain.entities.Road

interface RoadRepository : JpaRepository<Road, Long>
