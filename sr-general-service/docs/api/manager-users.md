# Управление пользователями

Базовый путь: `/api/manager/users`

**Авторизация:** только роль **MODERATOR**, заголовок `Authorization: Bearer <JWT>`. Иначе **401/403**.

## GET /api/manager/users

Список пользователей.

**Ответ:** 200 — массив UserManagerResponse (в т.ч. поле `role`: `USER` | `MODERATOR`)

```bash
curl -s -S "http://localhost/api/manager/users" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/manager/users/{id}

Пользователь по id.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/manager/users/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## POST /api/manager/users

Создание пользователя.

**Тело запроса:**

| Параметр       | Тип    | Обязательный | Описание     |
|----------------|--------|--------------|--------------|
| `telegramName` | string?| нет          | Telegram     |
| `firstname`    | string?| нет          | Имя          |
| `middlename`   | string?| нет          | Отчество     |
| `lastname`     | string?| нет          | Фамилия      |
| `department`   | string?| нет          | Отдел        |
| `city`         | string?| нет          | Город        |
| `phone`        | string?| нет          | Телефон      |
| `email`        | string?| нет          | Email        |
| `walletAddress`| string?| нет          | Адрес кошелька |

**Ответ:** 201, 404

```bash
curl -s -S -X POST "http://localhost/api/manager/users" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"firstname\":\"Иван\",\"lastname\":\"Иванов\",\"email\":\"manager-user@example.com\"}"
```

---

## PUT /api/manager/users/{id}

Обновление пользователя. Тело — как в POST.

**Ответ:** 200, 404

```bash
curl -s -S -X PUT "http://localhost/api/manager/users/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"firstname\":\"Петр\",\"lastname\":\"Петров\",\"email\":\"manager-user-updated@example.com\"}"
```

---

## DELETE /api/manager/users/{id}

Удаление пользователя.

Поведение при удалении:

- Если у пользователя есть отчёты, они **не удаляются**.
- Для таких отчётов `userId` становится `null`.
- Отчёты переводятся в статус `ARCHIEVED`.
- В `comment` добавляется техническая пометка об архивировании после удаления пользователя.
- Для каждого такого отчёта записывается новый blockchain snapshot хэш (`report-archived-anonymized|...`) и обновляется `blockchainTxHash`.
- Если после удаления пользователь регистрируется заново с теми же данными, это будет **новая сущность с новым userId** и новым user-hash в блокчейне. Старые архивные отчёты остаются привязаны к историческому snapshot после удаления и автоматически к новому пользователю не присоединяются.

**Ответ:** 204, 404

```bash
curl -s -S -X DELETE "http://localhost/api/manager/users/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
