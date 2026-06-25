# Кэширование на основе Redis

Приложение использует Redis для распределённого кэширования данных.

## Обзор

Кэширование реализовано с помощью Spring Cache Abstraction с Redis в качестве хранилища. Конфигурация определена в классе `CacheConfig`.

**Преимущества Redis перед Caffeine**:
- Распределённое кэширование (доступно для всех инстансов приложения)
- Персистентность данных (сохранение при перезапуске)
- Масштабируемость
- Поддержка кластеризации

---

## Конфигурация

### CacheConfig

**Расположение**: `src/main/kotlin/ru/cs/roadcheck/common/config/CacheConfig.kt`

**Параметры кэша**:
| Параметр | Значение | Описание |
|----------|----------|----------|
| `time-to-live` | 300000 мс (5 мин) | Время жизни записи в кэше |
| `cache-null-values` | false | Не кэшировать null значения |
| `key-prefix` | `roadcheck:` | Префикс для всех ключей кэша |

**Сериализация**:
- Ключи: `StringRedisSerializer` (строковый формат)
- Значения: `GenericJackson2JsonRedisSerializer` (JSON формат)

---

### Настройка подключения

#### application.yml (production)

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
      key-prefix: "roadcheck:"
```

#### application-local.yml (local)

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms

  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
      key-prefix: "roadcheck:"
```

---

## Docker Compose

Для локальной разработки Redis запускается через Docker Compose.

**Файл**: `src/main/resources/docker-compose-local.yml`

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: roadcheck-general-redis-local
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5
    command: redis-server --appendonly yes
```

**Запуск локального окружения**:
```bash
docker-compose -f src/main/resources/docker-compose-local.yml up -d
```

---

## Использование

### Аннотация @Cacheable

Кэширование результата метода:

```kotlin
@Cacheable(value = ["regions"], key = "#id")
fun getRegionById(id: Long): Region? {
    return regionRepository.findById(id)
}
```

### Аннотация @CacheEvict

Очистка кэша при изменении данных:

```kotlin
@CacheEvict(value = ["regions"], key = "#id")
fun updateRegion(id: Long, region: Region) {
    regionRepository.save(region)
}
```

### Аннотация @CachePut

Обновление кэша без пропуска выполнения метода:

```kotlin
@CachePut(value = ["regions"], key = "#result.id")
fun createRegion(region: Region): Region {
    return regionRepository.save(region)
}
```

---

## Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `REDIS_HOST` | Хост Redis | `localhost` |
| `REDIS_PORT` | Порт Redis | `6379` |
| `REDIS_PASSWORD` | Пароль Redis | (пусто) |

---

## Мониторинг

### Health Check

Spring Actuator предоставляет endpoint для проверки состояния Redis:

```bash
curl http://localhost/actuator/health
```

Ответ:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP"
    }
  }
}
```

### Redis CLI

Подключение к Redis для отладки:

```bash
docker exec -it roadcheck-general-redis-local redis-cli
```

---

## Зависимости

В `dependencies.gradle` указаны необходимые зависимости:

```kotlin
// Caching - Redis
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("io.lettuce:lettuce-core")
```

**Lettuce** — асинхронный Redis-клиент, используемый Spring Boot по умолчанию.
