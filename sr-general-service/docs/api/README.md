# RoadCheck API

Базовый URL через nginx: `http://localhost` (порт `80`)  
Swagger UI: `http://localhost/swagger-ui/index.html` (или `http://localhost/swagger-ui.html`)  
OpenAPI JSON: `http://localhost/v3/api-docs`  
Прямой URL приложения (диагностика): `http://localhost:8081`

Для production-профиля (`prod`) OpenAPI/Swagger server URL автоматически подставляет `HOST_IP` из окружения.

## Общие сведения

- Формат: **JSON**, кодировка **UTF-8**.
- Для методов с телом: `Content-Type: application/json`.
- Проверка `X-From-Nginx` включена всегда: при прямом запросе на `8081` обязательно передавайте `X-From-Nginx: 1`.
- Роли: `USER` и `MODERATOR` (подробно в [Авторизация](auth.md)).
- JWT: для защищённых ручек используйте `Authorization: Bearer <token>`.

## Доступ к ручкам

### 1) Доступно всем (без JWT)

- `/api/auth/**`
- `/api/regions/**`
- `GET /api/predictions/charts/**`

Примеры `curl`:

```bash
curl -s -S "http://localhost/api/regions"
```

```bash
curl -s -S -X POST "http://localhost/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"login\":\"demo_user\",\"password\":\"secret12\"}"
```

### 2) Доступно аутентифицированным (`USER` и `MODERATOR`)

- `/api/reports/**` (кроме модераторских `confirm/decline`)
- `/api/dashboard/**`
- `/api/analytics/**`
- `/api/auth/vk-id`, `/api/auth/logout`

Пример `curl`:

```bash
curl -s -S "http://localhost/api/reports?page=1&size=20" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

### 3) Доступно только модераторам (`MODERATOR`)

- `/api/manager/**`
- `PUT /api/reports/{id}/confirm`
- `PUT /api/reports/{id}/decline`
- `POST /api/predictions/run-manual`

Пример `curl`:

```bash
curl -s -S -X PUT "http://localhost/api/reports/15/confirm" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"comment\":\"Подтверждено модератором\"}"
```

## Формат ошибок

| Поле      | Тип    | Описание     |
|-----------|--------|--------------|
| `message` | string | Текст ошибки |
| `status`  | number | HTTP-код     |

## Разделы API

- [Авторизация](auth.md)
- [Дашборд](dashboard.md)
- [Аналитика](analytics.md)
- [Отчёты](reports.md)
- [Регионы (публичный API)](regions.md)
- [Предсказания и графики](predictions.md)
- [Управление пользователями](manager-users.md)
- [Управление отчётами](manager-reports.md)
- [Управление регионами](manager-regions.md)
- [Управление дорогами](manager-roads.md)
- [Управление опасными зонами](manager-dangerous-zones.md)
