package ru.cs.roadcheck.rest.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.rest.GlobalExceptionHandler
import ru.cs.roadcheck.rest.dto.BindVkIdRequest
import ru.cs.roadcheck.rest.dto.LoginRequest
import ru.cs.roadcheck.rest.dto.LoginResponse
import ru.cs.roadcheck.rest.dto.RegisterRequest
import ru.cs.roadcheck.rest.dto.RegisterResponse
import ru.cs.roadcheck.service.AuthService

class AuthControllerTest {

    private val authService: AuthService = mockk()
    private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        mockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `register returns created`() {
        val req = RegisterRequest(
            login = "u1",
            password = "secret12",
            email = "a@b.co",
            phone = "+79990001122",
        )
        val res = RegisterResponse(
            success = true,
            message = "ok",
            userId = 1L,
            token = "jwt",
            blockchainVerified = false,
            role = "USER",
        )
        every { authService.register(any()) } returns res

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.token").value("jwt"))

        verify { authService.register(any()) }
    }

    @Test
    fun `login returns ok when success`() {
        val req = LoginRequest(login = "u1", password = "secret12")
        every { authService.login(any()) } returns LoginResponse(
            success = true,
            message = "ok",
            token = "jwt",
            userId = 1L,
            role = "USER",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `login returns unauthorized when failed`() {
        val req = LoginRequest(login = "u1", password = "wrong")
        every { authService.login(any()) } returns LoginResponse(
            success = false,
            message = "bad",
            token = null,
            userId = null,
            role = null,
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout returns ok`() {
        every { authService.logout() } returns LoginResponse(
            success = true,
            message = "bye",
            token = null,
            userId = null,
            role = null,
        )

        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk)
    }

    @Test
    fun `bindVkId returns ok`() {
        every { authService.bindVkId(5L, any()) } returns LoginResponse(
            success = true,
            message = "ok",
            token = "jwt",
            userId = 5L,
            role = "USER",
        )
        val token = UsernamePasswordAuthenticationToken(
            5L,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

        val ctrl = AuthController(authService)
        val response = ctrl.bindVkId(token, BindVkIdRequest("vk123"))

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body?.success).isTrue()

        verify { authService.bindVkId(5L, any()) }
    }
}
