package ru.cs.roadcheck.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.auth.JwtService
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.BotRegisterRequest
import ru.cs.roadcheck.rest.dto.BotUserExistsResponse
import ru.cs.roadcheck.rest.dto.LoginRequest
import ru.cs.roadcheck.rest.dto.LoginResponse
import ru.cs.roadcheck.rest.dto.BindVkIdRequest
import ru.cs.roadcheck.rest.dto.RegisterRequest
import ru.cs.roadcheck.rest.dto.RegisterResponse
import java.security.MessageDigest
import ru.cs.roadcheck.common.domain.entities.User
import ru.cs.roadcheck.common.domain.entities.UserRole

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val blockchainService: BlockchainService,
) {

    @Transactional
    fun registerByBot(request: BotRegisterRequest): RegisterResponse {
        val registerRequest = RegisterRequest(
            login = request.login,
            password = request.password,
            email = request.email,
            firstname = request.firstname,
            lastname = request.lastname,
            phone = request.phone,
            walletAddress = request.walletAddress,
        )

        val response = register(registerRequest)
        val userId = response.userId ?: throw ValidationException("Пользователь не был создан")
        val user = userRepository.findById(userId)
            .orElseThrow { ValidationException("Пользователь не найден после регистрации") }

        val vkId = request.vkId.trim()
        if (vkId.isBlank()) throw ValidationException("vkId не может быть пустым")
        userRepository.findByVkId(vkId)?.let { existing ->
            if (existing.id != user.id) {
                throw ValidationException("Этот vkId уже привязан к другому аккаунту")
            }
        }

        user.vkId = vkId
        var saved = userRepository.save(user)
        if (blockchainService.isAvailable()) {
            val contentHash = buildUserContentHash(saved)
            blockchainService.recordUser(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved = userRepository.save(saved)
            }
        }

        val token = jwtService.generateToken(saved.id!!, saved.login ?: saved.vkId ?: saved.email ?: saved.phone ?: "", saved.role)
        return response.copy(
            userId = saved.id,
            token = token,
            blockchainVerified = saved.blockchainVerified ?: false,
            role = saved.role.name,
        )
    }

    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        val login = request.login.trim().lowercase()
        val email = request.email.trim().lowercase()
        val phone = request.phone.trim()
        if (login.isBlank()) throw ValidationException("Логин обязателен")
        if (login.length < 3 || login.length > 100) throw ValidationException("Логин от 3 до 100 символов")
        if (email.isBlank()) throw ValidationException("Email обязателен")
        if (phone.isBlank()) throw ValidationException("Телефон обязателен")
        if (request.password.isBlank()) throw ValidationException("Пароль обязателен")
        if (request.password.length < 6) throw ValidationException("Пароль минимум 6 символов")
        if (!email.contains("@") || !email.contains(".")) throw ValidationException("Некорректный email")
        if (userRepository.findByLogin(login) != null) {
            throw ValidationException("Логин уже занят")
        }
        if (userRepository.findByEmail(email) != null) {
            throw ValidationException("Email уже зарегистрирован")
        }
        if (userRepository.findByPhone(phone) != null) {
            throw ValidationException("Телефон уже зарегистрирован")
        }
        val user = User().apply {
            this.login = login
            this.email = email
            firstname = request.firstname?.trim()?.takeIf { it.isNotBlank() }
            lastname = request.lastname?.trim()?.takeIf { it.isNotBlank() }
            this.phone = phone
            walletAddress = request.walletAddress?.trim()?.takeIf { it.isNotBlank() }
            passwordHash = passwordEncoder.encode(request.password)
            role = UserRole.USER
        }
        var saved = userRepository.save(user)
        if (blockchainService.isAvailable()) {
            val contentHash = buildUserContentHash(saved)
            blockchainService.recordUser(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved = userRepository.save(saved)
            }
        }
        val token = jwtService.generateToken(saved.id!!, login, saved.role)
        return RegisterResponse(
            success = true,
            message = "Регистрация успешна",
            userId = saved.id,
            token = token,
            blockchainVerified = saved.blockchainVerified ?: false,
            role = saved.role.name,
        )
    }

    fun login(request: LoginRequest): LoginResponse {
        val identifier = request.login.trim()
        if (identifier.isBlank()) throw ValidationException("Логин не может быть пустым")
        if (request.password.isBlank()) throw ValidationException("Пароль не может быть пустым")

        val user = userRepository.findForAuth(identifier)
            ?: return LoginResponse(success = false, message = "Неверный логин или пароль")

        val hash = user.passwordHash
        if (hash == null || !passwordEncoder.matches(request.password, hash)) {
            return LoginResponse(success = false, message = "Неверный логин или пароль")
        }

        val token = jwtService.generateToken(user.id!!, user.login ?: user.email ?: user.phone ?: user.vkId ?: "", user.role)
        return LoginResponse(
            success = true,
            message = "Успешный вход",
            userId = user.id,
            token = token,
            role = user.role.name,
        )
    }

    @Transactional
    fun bindVkId(userId: Long, request: BindVkIdRequest): LoginResponse {
        val vkId = request.vkId.trim()
        if (vkId.isBlank()) throw ValidationException("vkId не может быть пустым")

        userRepository.findByVkId(vkId)?.let { existing ->
            if (existing.id != userId) {
                throw ValidationException("Этот vkId уже привязан к другому аккаунту")
            }
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ValidationException("Пользователь не найден") }
        user.vkId = vkId
        var saved = userRepository.save(user)
        if (blockchainService.isAvailable()) {
            val contentHash = buildUserContentHash(saved)
            blockchainService.recordUser(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved = userRepository.save(saved)
            }
        }
        return LoginResponse(success = true, message = "VK ID успешно привязан", userId = user.id, role = user.role.name)
    }

    fun logout() = LoginResponse(success = true, message = "Выход выполнен")

    fun checkUserExistsByVkId(vkId: String): BotUserExistsResponse {
        val normalizedVkId = vkId.trim()
        if (normalizedVkId.isBlank()) throw ValidationException("vkId не может быть пустым")
        return BotUserExistsResponse(
            vkId = normalizedVkId,
            exists = userRepository.findByVkId(normalizedVkId) != null,
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildUserContentHash(user: User): String =
        sha256("${user.id}|${user.login}|${user.email}|${user.phone}|${user.vkId}|${user.walletAddress}|${user.createdAt}")
}
