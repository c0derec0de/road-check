# Bot API (Internal)

Документация по внутреннему API для интеграции с ботом.

## Аутентификация

Все методы API бота требуют заголовок `X-API-TOKEN` с сервисным токеном. Токен настраивается **только** через переменную окружения `BOT_API_TOKEN`.

**Важно:** Обычные JWT токены пользователей не работают с этими эндпоинтами.

## Конфигурация

### 1. Переменные окружения (.env)

```bash
BOT_API_TOKEN=<your-bot-token>
```

### 2. application.yml

```yaml
bot:
  api-token: ${BOT_API_TOKEN:X-API-TOKEN}
```

### 3. BotProperties

```kotlin
@ConfigurationProperties("bot")
data class BotProperties(
    val apiToken: String = "X-API-TOKEN",
)
```

### 4. Использование в коде

```kotlin
class BotController(
    private val botProperties: BotProperties,
) {
    private fun validateApiToken(apiToken: String) {
        if (apiToken != botProperties.apiToken) {
            throw ValidationException("Неверный X-API-TOKEN")
        }
    }
}
```

### 5. Формат токена

Токен должен соответствовать формату: `s.<32 символа>`

Пример: `s.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

Nginx проверяет токен через regex: `^s\.[a-zA-Z0-9]{32}$`

## Эндпоинты

### 1. Регистрация пользователя ботом

**POST** `/api/internal/bot/register`

Регистрирует нового пользователя с автоматической привязкой VK ID.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Тело запроса

```json
{
  "login": "bot_user_123",
  "password": "password123",
  "email": "user@example.com",
  "vkId": "12345678",
  "firstname": "Иван",
  "lastname": "Иванов",
  "phone": "+79991234567",
  "walletAddress": "0x1234567890abcdef"
}
```

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| login | string | Да | Логин пользователя (3-100 символов) |
| password | string | Да | Пароль пользователя (минимум 6 символов) |
| email | string | Да | Email пользователя |
| vkId | string | Да | VK ID пользователя |
| firstname | string | Нет | Имя пользователя |
| lastname | string | Нет | Фамилия пользователя |
| phone | string | Да | Телефон пользователя |
| walletAddress | string | Нет | Адрес кошелька в блокчейне |

#### Ответ

**201 Created**

```json
{
  "success": true,
  "message": "Регистрация через бота успешна",
  "userId": 1,
  "token": "jwt-token-here",
  "blockchainVerified": false,
  "role": "USER"
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Ошибка валидации данных или неверный токен |
| 409 | Пользователь с таким логином/email/телефоном/VK ID уже существует |

---

### 2. Создание отчёта ботом

**POST** `/api/internal/bot/reports`

Создаёт новый отчёт от имени пользователя по его VK ID.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Тело запроса

```json
{
  "vkId": "12345678",
  "incidentType": "Повреждение дорожного покрытия",
  "description": "Большая яма на проезжей части",
  "latitude": 55.755826,
  "longitude": 37.617299,
  "regionId": 1,
  "photoUrl": "https://example.com/photo.jpg"
}
```

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| vkId | string | Да | VK ID пользователя |
| incidentType | string | Да | Тип инцидента |
| description | string | Да | Описание инцидента |
| latitude | number | Да | Широта места инцидента |
| longitude | number | Да | Долгота места инцидента |
| regionId | integer | Нет | ID региона |
| photoUrl | string | Нет | URL фотографии |

#### Ответ

**201 Created**

```json
{
  "id": 1,
  "userId": 1,
  "incidentType": "Повреждение дорожного покрытия",
  "description": "Большая яма на проезжей части",
  "latitude": 55.755826,
  "longitude": 37.617299,
  "regionId": 1,
  "status": "NEW",
  "createdAt": "2026-03-05T12:00:00Z",
  "blockchainVerified": false
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Ошибка валидации данных или неверный токен |
| 404 | Пользователь с указанным VK ID не найден |

---

### 3. Получение списка отчётов пользователя (кратко, с пагинацией)

**GET** `/api/internal/bot/reports?vkId={vkId}&page={page}&size={size}`

Получает **постраничный** список отчётов пользователя по его VK ID (краткие элементы списка, не полная карточка). Чтобы загрузить **все** отчёты сразу в формате полной карточки, используйте раздел 4 — `GET /api/internal/bot/reports/detailed-all`.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Параметры запроса

| Параметр | Тип | Обязательный | По умолчанию | Описание |
|----------|-----|--------------|--------------|----------|
| vkId | string | Да | - | VK ID пользователя |
| page | integer | Нет | 1 | Номер страницы |
| size | integer | Нет | 20 | Размер страницы |

#### Ответ

**200 OK**

```json
{
  "reports": [
    {
      "id": 1,
      "incidentType": "Повреждение дорожного покрытия",
      "description": "Большая яма",
      "latitude": 55.755826,
      "longitude": 37.617299,
      "status": "NEW",
      "createdAt": "2026-03-05T12:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalItems": 1,
    "itemsPerPage": 20
  },
  "filters": {
    "availableStatuses": ["NEW", "IN_PROGRESS", "CONFIRMED"],
    "availableRiskLevels": ["high", "medium", "low"]
  }
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Неверный токен |
| 404 | Пользователь с указанным VK ID не найден |

---

### 4. Все отчёты пользователя (детальные карточки)

**GET** `/api/internal/bot/reports/detailed-all?vkId={vkId}`

Возвращает **все неархивированные** отчёты пользователя в формате полной карточки (`ReportDetailResponse`), от новых к старым. Если отчётов нет, ответ **200** с `reports: []` и `total: 0`.

Для **краткого** постраничного списка (элементы списка без полной карточки) используйте раздел 3 — `GET /api/internal/bot/reports`.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Параметры запроса

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| vkId | string | Да | VK ID пользователя |

#### Ответ

**200 OK**

```json
{
  "reports": [
    {
      "id": 1,
      "title": "Повреждение дорожного покрытия",
      "address": null,
      "description": "Большая яма",
      "comment": null,
      "status": "NEW",
      "riskLevel": null,
      "isDangerousZone": false,
      "user": null,
      "createdAt": "2026-03-05T12:00:00Z",
      "updatedAt": null,
      "photos": [],
      "location": { "lat": 55.755826, "lng": 37.617299 },
      "blockchainTxHash": null,
      "blockchainVerified": false,
      "blockchainBlockNumber": null,
      "comments": []
    }
  ],
  "total": 1
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Неверный токен или пустой vkId |
| 404 | Пользователь с указанным VK ID не найден |

---

### 5. Получение детальной информации об отчёте

**GET** `/api/internal/bot/reports/{id}`

Получает полную информацию об отчёте по его ID.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Параметры пути

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| id | long | Да | ID отчёта |

#### Ответ

**200 OK**

```json
{
  "id": 1,
  "title": "Повреждение дорожного покрытия",
  "address": null,
  "description": "Большая яма на проезжей части",
  "comment": null,
  "status": "NEW",
  "riskLevel": null,
  "isDangerousZone": false,
  "user": null,
  "createdAt": "2026-03-05T12:00:00Z",
  "updatedAt": null,
  "photos": [],
  "location": { "lat": 55.755826, "lng": 37.617299 },
  "blockchainTxHash": null,
  "blockchainVerified": false,
  "blockchainBlockNumber": null,
  "comments": []
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Неверный токен |
| 404 | Отчёт с указанным ID не найден |

---

### 6. Проверка существования пользователя по VK ID

**GET** `/api/internal/bot/users/exists?vkId={vkId}`

Возвращает, есть ли пользователь с указанным `vkId`.

#### Заголовки
| Название | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| X-API-TOKEN | string | Да | Сервисный токен бота |

#### Пример ответа

```json
{
  "vkId": "12345678",
  "exists": true
}
```

#### Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Пустой vkId |
| 401 | Неверный токен |

---

## Примеры использования

### cURL

#### Регистрация пользователя

```bash
curl -X POST http://localhost/api/internal/bot/register \
  -H "X-API-TOKEN: <your-bot-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

#### Создание отчёта

```bash
curl -X POST http://localhost/api/internal/bot/reports \
  -H "X-API-TOKEN: <your-bot-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

#### Получение отчётов пользователя

```bash
curl -X GET "http://localhost/api/internal/bot/reports?vkId=12345678&page=1&size=20" \
  -H "X-API-TOKEN: <your-bot-token>"
```

#### Все отчёты пользователя (детальные карточки)

```bash
curl -X GET "http://localhost/api/internal/bot/reports/detailed-all?vkId=12345678" \
  -H "X-API-TOKEN: <your-bot-token>"
```

#### Получение детальной информации об отчёте

```bash
curl -X GET "http://localhost/api/internal/bot/reports/1" \
  -H "X-API-TOKEN: <your-bot-token>"
```

#### Проверка наличия пользователя по VK ID

```bash
curl -X GET "http://localhost/api/internal/bot/users/exists?vkId=12345678" \
  -H "X-API-TOKEN: <your-bot-token>"
```

## Безопасность

1. **Храните токен в секрете** - не коммитьте токен в репозиторий
2. **Используйте HTTPS** в production
3. **Регулярно меняйте токен** в production окружении
4. **Ограничьте доступ** к эндпоинтам на уровне сети (firewall, VPC)
5. **Измените токен в production** - сгенерируйте новый уникальный токен

## Отличия от обычного API

| Характеристика | Обычный API | Bot API |
|----------------|-------------|---------|
| Аутентификация | JWT токен | X-API-TOKEN |
| Регистрация | С паролем | С паролем + VK ID |
| Создание отчёта | По JWT токену | По VK ID |
| Доступ | Для авторизованных пользователей | Только для бота с сервисным токеном |
