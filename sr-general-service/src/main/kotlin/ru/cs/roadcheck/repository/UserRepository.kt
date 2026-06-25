package ru.cs.roadcheck.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.cs.roadcheck.common.domain.entities.User
import java.time.Instant

interface UserRepository : JpaRepository<User, Long> {

    fun findByLogin(login: String): User?

    fun findByEmail(email: String): User?

    fun findByVkId(vkId: String): User?

    fun findByPhone(phone: String): User?

    @Query(
        value = """
            select * from public.users
            where lower(login) = lower(:identifier)
               or lower(email) = lower(:identifier)
               or lower(vk_id) = lower(:identifier)
               or phone = :identifier
            limit 1
        """,
        nativeQuery = true
    )
    fun findForAuth(@Param("identifier") identifier: String): User?

    @Query(
        value = """
            select count(*) 
            from public.users 
            where created_at > :since
        """,
        nativeQuery = true
    )
    fun countByCreatedAtAfter(@Param("since") since: Instant): Long

    @Query(
        value = """
            select count(*) 
            from public.users 
            where created_at between :start and :end
        """,
        nativeQuery = true
    )
    fun countByCreatedAtBetween(@Param("start") start: Instant, @Param("end") end: Instant): Long
}
