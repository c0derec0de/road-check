# Отчёты

Требуется заголовок `Authorization: Bearer <JWT>` (роль **USER** или **MODERATOR**).

- **MODERATOR:** видит все отчёты, поле `userId` в теле создания — реальный заявитель.
- **USER:** в списке и деталях только отчёты, где `user_id` совпадает с ID из токена; при создании поле `userId` в теле **игнорируется**, заявителем всегда становится текущий пользователь.

Подтверждение/отклонение (`PUT .../confirm`, `PUT .../decline`) — только **MODERATOR** (иначе **403**).

Пример `curl` (доступ для `USER` и `MODERATOR`):

```bash
curl -s -S "http://localhost/api/reports?page=1&size=20" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

## POST /api/reports

Создание отчёта. Статус по умолчанию: NEW.

**Тело запроса:**

| Параметр       | Тип     | Обязательный | Описание               |
|----------------|---------|--------------|------------------------|
| `policeUserId` | long    | да           | ID сотрудника полиции  |
| `userId`       | long    | да*          | ID заявителя (*для **USER** подставляется из токена) |
| `incidentType` | string  | да           | Тип инцидента          |
| `latitude`     | number? | нет          | Широта                 |
| `longitude`    | number? | нет          | Долгота                |
| `description`  | string? | нет          | Описание               |
| `photosUuid`   | string? | нет          | UUID фотографий        |
| `fatalities`   | int?    | нет          | Погибшие               |
| `injuries`     | int?    | нет          | Пострадавшие           |
| `cause`        | string? | нет          | Причина                |

**Ответ:** 201 — создан, 400 — ошибка валидации, **401** — нет JWT

```bash
curl -s -S -X POST "http://localhost/api/reports" \
  -H "Authorization: Bearer YOUR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"policeUserId\":1,\"userId\":1,\"incidentType\":\"ДТП\",\"description\":\"Описание инцидента\"}"
```

---

## GET /api/reports

Список с пагинацией. Для **USER** — только его обращения.
Отчёты со статусом `ARCHIEVED` в выдачу не попадают. Отчёты со статусом `DECLINED` возвращаются как обычно.

**Query:** 

| Параметр    | Тип    | Описание                                                |
|-------------|--------|---------------------------------------------------------|
| `page`      | int    | Номер страницы (по умолчанию 1)                         |
| `size`      | int    | Размер страницы (по умолчанию 20)                       |
| `status`    | string | Фильтр по статусу (`NEW`, `IN_PROGRESS`, `CONFIRMED`, `DECLINED`)   |
| `riskLevel` | string | Фильтр по уровню риска (`high`, `medium`, `low`)        |
| `regionId`  | long   | Фильтр по региону                                       |

**Ответ:** 200 — reports, pagination, filters; **401** — нет JWT

---

## GET /api/reports/{id}

Детали отчёта. **USER** получает **404**, если отчёт принадлежит другому пользователю.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/reports/1" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

---

## PUT /api/reports/{id}/confirm

Подтверждение отчёта.

**Тело:** `{ "comment": "Обязательный комментарий модератора" }`

**Ответ:** 200, 400, 404; **403** — не модератор; **401** — нет JWT

```bash
curl -s -S -X PUT "http://localhost/api/reports/1/confirm" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"comment\":\"Подтверждено\"}"
```

---

## PUT /api/reports/{id}/decline

Отклонение отчёта.

**Тело:** не требуется

**Ответ:** 200, 404; **403** — не модератор; **401** — нет JWT

```bash
curl -s -S -X PUT "http://localhost/api/reports/1/decline" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
