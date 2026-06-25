# Управление отчётами

Базовый путь: `/api/manager/reports`

**Авторизация:** JWT, роль **MODERATOR** (`Authorization: Bearer`).

## GET /api/manager/reports

Список отчётов.

**Ответ:** 200 — массив ReportResponse

```bash
curl -s -S "http://localhost/api/manager/reports" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/manager/reports/{id}

Отчёт по id.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/manager/reports/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## POST /api/manager/reports

Создание отчёта. Требует существующие policeUserId и userId.

**Тело запроса:**

| Параметр       | Тип     | Обязательный | Описание       |
|----------------|---------|--------------|----------------|
| `policeUserId` | long    | да           | ID сотрудника  |
| `userId`       | long    | да           | ID заявителя   |
| `incidentType` | string  | да           | Тип инцидента  |
| `latitude`     | number? | нет          | Широта         |
| `longitude`    | number? | нет          | Долгота        |
| `description`  | string? | нет          | Описание       |
| `photosUuid`   | string? | нет          | UUID фото      |
| `status`       | string? | нет          | NEW, IN_PROGRESS, CONFIRMED, DECLINED, ARCHIEVED |
| `fatalities`   | int?    | нет          | Погибшие       |
| `injuries`     | int?    | нет          | Пострадавшие   |
| `cause`        | string? | нет          | Причина        |
| `riskLevel`    | string? | нет          | high/medium/low|
| `title`        | string? | нет          | Заголовок      |
| `address`      | string? | нет          | Адрес          |
| `isDangerousZone` | bool? | нет        | Опасная зона   |

**Ответ:** 201, 400, 404

```bash
curl -s -S -X POST "http://localhost/api/manager/reports" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"policeUserId\":1,\"userId\":1,\"incidentType\":\"Яма на дороге\",\"description\":\"Глубокая яма\"}"
```

---

## PUT /api/manager/reports/{id}

Обновление отчёта. Тело — как в POST.

**Ответ:** 200, 400, 404

```bash
curl -s -S -X PUT "http://localhost/api/manager/reports/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"policeUserId\":1,\"userId\":1,\"incidentType\":\"Яма на дороге\",\"status\":\"IN_PROGRESS\"}"
```

---

## DELETE /api/manager/reports/{id}

Удаление отчёта.

**Ответ:** 204, 404

```bash
curl -s -S -X DELETE "http://localhost/api/manager/reports/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
