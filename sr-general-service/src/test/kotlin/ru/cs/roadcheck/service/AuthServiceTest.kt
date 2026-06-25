package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import ru.cs.roadcheck.auth.JwtService
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.blockchain.BlockchainRecordResult
import ru.cs.roadcheck.common.domain.entities.User
import ru.cs.roadcheck.common.domain.entities.UserRole
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.BindVkIdRequest
import ru.cs.roadcheck.rest.dto.LoginRequest
import ru.cs.roadcheck.rest.dto.RegisterRequest
import java.time.Instant
import java.util.Optional

class AuthServiceTest {

    private val userRepository: UserRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val jwtService: JwtService = mockk()
    private val blockchainService: BlockchainService = mockk()

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            jwtService = jwtService,
            blockchainService = blockchainService,
        )
    }

    @Test
    fun `register creates user with valid data`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
            firstname = "Test",
            lastname = "User",
            walletAddress = null,
        )

        every { userRepository.findByLogin("testuser") } returns null
        every { userRepository.findByEmail("test@example.com") } returns null
        every { userRepository.findByPhone("+79991234567") } returns null
        every { passwordEncoder.encode("password123") } returns "encodedHash"
        every { blockchainService.isAvailable() } returns false
        every { jwtService.generateToken(any(), "testuser", UserRole.USER) } returns "jwt-token"

        val savedUser = User().apply {
            id = 1L
            login = "testuser"
            email = "test@example.com"
            phone = "+79991234567"
            firstname = "Test"
            lastname = "User"
            passwordHash = "encodedHash"
            role = UserRole.USER
        }
        every { userRepository.save(any()) } returns savedUser

        val result = authService.register(request)

        assertThat(result.success).isTrue()
        assertThat(result.userId).isEqualTo(1L)
        assertThat(result.token).isEqualTo("jwt-token")
        assertThat(result.blockchainVerified).isFalse()
        assertThat(result.role).isEqualTo("USER")

        verify { userRepository.save(any()) }
        verify { jwtService.generateToken(any(), "testuser", UserRole.USER) }
    }

    @Test
    fun `register throws when login is blank`() {
        val request = RegisterRequest(
            login = "   ",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Логин обязателен")
    }

    @Test
    fun `register throws when login is too short`() {
        val request = RegisterRequest(
            login = "ab",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Логин от 3 до 100 символов")
    }

    @Test
    fun `register throws when email is blank`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "   ",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Email обязателен")
    }

    @Test
    fun `register throws when phone is blank`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "   ",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Телефон обязателен")
    }

    @Test
    fun `register throws when password is blank`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "   ",
            email = "test@example.com",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Пароль обязателен")
    }

    @Test
    fun `register throws when password is too short`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "12345",
            email = "test@example.com",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Пароль минимум 6 символов")
    }

    @Test
    fun `register throws when email is invalid`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "invalid-email",
            phone = "+79991234567",
        )

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Некорректный email")
    }

    @Test
    fun `register throws when login is taken`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        every { userRepository.findByLogin("testuser") } returns User()

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Логин уже занят")
    }

    @Test
    fun `register throws when email is registered`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        every { userRepository.findByEmail("test@example.com") } returns User()
        every { userRepository.findByLogin("testuser") } returns null

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Email уже зарегистрирован")
    }

    @Test
    fun `register throws when phone is registered`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        every { userRepository.findByPhone("+79991234567") } returns User()
        every { userRepository.findByLogin("testuser") } returns null
        every { userRepository.findByEmail("test@example.com") } returns null

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Телефон уже зарегистрирован")
    }

    @Test
    fun `register with blockchain saves transaction hash`() {
        val request = RegisterRequest(
            login = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "+79991234567",
        )

        every { userRepository.findByLogin("testuser") } returns null
        every { userRepository.findByEmail("test@example.com") } returns null
        every { userRepository.findByPhone("+79991234567") } returns null
        every { passwordEncoder.encode("password123") } returns "encodedHash"
        every { blockchainService.isAvailable() } returns true
        every { blockchainService.recordUser(any(), any()) } returns BlockchainRecordResult("tx-hash", 123L)
        every { jwtService.generateToken(any(), "testuser", UserRole.USER) } returns "jwt-token"

        val savedUser = User().apply {
            id = 1L
            login = "testuser"
            blockchainVerified = true
            blockchainTxHash = "tx-hash"
            role = UserRole.USER
            createdAt = Instant.now()
        }
        every { userRepository.save(any()) } returnsMany listOf(
            User().apply {
                id = 1L
                login = "testuser"
                role = UserRole.USER
                createdAt = Instant.now()
            },
            savedUser,
        )

        val result = authService.register(request)

        assertThat(result.blockchainVerified).isTrue()
        verify { blockchainService.recordUser(any(), any()) }
    }

    @Test
    fun `login with valid credentials returns token`() {
        val request = LoginRequest(login = "testuser", password = "password123")
        val user = User().apply {
            id = 1L
            login = "testuser"
            passwordHash = "encodedHash"
            role = UserRole.USER
        }

        every { userRepository.findForAuth("testuser") } returns user
        every { passwordEncoder.matches("password123", "encodedHash") } returns true
        every { jwtService.generateToken(1L, "testuser", UserRole.USER) } returns "jwt-token"

        val result = authService.login(request)

        assertThat(result.success).isTrue()
        assertThat(result.userId).isEqualTo(1L)
        assertThat(result.token).isEqualTo("jwt-token")
        assertThat(result.role).isEqualTo("USER")
    }

    @Test
    fun `login with invalid login returns failure`() {
        val request = LoginRequest(login = "unknown", password = "password123")

        every { userRepository.findForAuth("unknown") } returns null

        val result = authService.login(request)

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Неверный логин или пароль")
    }

    @Test
    fun `login with invalid password returns failure`() {
        val request = LoginRequest(login = "testuser", password = "wrongpassword")
        val user = User().apply {
            id = 1L
            login = "testuser"
            passwordHash = "encodedHash"
            role = UserRole.USER
        }

        every { userRepository.findForAuth("testuser") } returns user
        every { passwordEncoder.matches("wrongpassword", "encodedHash") } returns false

        val result = authService.login(request)

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Неверный логин или пароль")
    }

    @Test
    fun `login with email identifier`() {
        val request = LoginRequest(login = "test@example.com", password = "password123")
        val user = User().apply {
            id = 1L
            email = "test@example.com"
            passwordHash = "encodedHash"
            role = UserRole.USER
        }

        every { userRepository.findForAuth("test@example.com") } returns user
        every { passwordEncoder.matches("password123", "encodedHash") } returns true
        every { jwtService.generateToken(1L, "test@example.com", UserRole.USER) } returns "jwt-token"

        val result = authService.login(request)

        assertThat(result.success).isTrue()
    }

    @Test
    fun `login with phone identifier`() {
        val request = LoginRequest(login = "+79991234567", password = "password123")
        val user = User().apply {
            id = 1L
            phone = "+79991234567"
            passwordHash = "encodedHash"
            role = UserRole.USER
        }

        every { userRepository.findForAuth("+79991234567") } returns user
        every { passwordEncoder.matches("password123", "encodedHash") } returns true
        every { jwtService.generateToken(1L, "+79991234567", UserRole.USER) } returns "jwt-token"

        val result = authService.login(request)

        assertThat(result.success).isTrue()
    }

    @Test
    fun `bindVkId successfully binds vkId`() {
        val request = BindVkIdRequest(vkId = "12345")
        val user = User().apply {
            id = 1L
            login = "testuser"
            role = UserRole.USER
        }

        every { userRepository.findByVkId("12345") } returns null
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user
        every { blockchainService.isAvailable() } returns false

        val result = authService.bindVkId(1L, request)

        assertThat(result.success).isTrue()
        assertThat(result.userId).isEqualTo(1L)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `bindVkId throws when vkId is blank`() {
        val request = BindVkIdRequest(vkId = "   ")

        assertThatThrownBy { authService.bindVkId(1L, request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("vkId не может быть пустым")
    }

    @Test
    fun `bindVkId throws when vkId is already bound to another user`() {
        val request = BindVkIdRequest(vkId = "12345")
        val existingUser = User().apply { id = 2L }

        every { userRepository.findByVkId("12345") } returns existingUser

        assertThatThrownBy { authService.bindVkId(1L, request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Этот vkId уже привязан к другому аккаунту")
    }

    @Test
    fun `bindVkId throws when user not found`() {
        val request = BindVkIdRequest(vkId = "12345")

        every { userRepository.findByVkId("12345") } returns null
        every { userRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy { authService.bindVkId(1L, request) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessage("Пользователь не найден")
    }

    @Test
    fun `logout returns success`() {
        val result = authService.logout()

        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Выход выполнен")
    }
}
