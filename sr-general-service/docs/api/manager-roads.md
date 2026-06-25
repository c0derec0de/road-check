# Управление дорогами

Базовый путь: `/api/manager/roads`

**Авторизация:** JWT, роль **MODERATOR**.

## GET /api/manager/roads

Список дорог.

**Ответ:** 200 — массив RoadManagerResponse

```bash
curl -s -S "http://localhost/api/manager/roads" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/manager/roads/{id}

Дорога по id.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/manager/roads/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## POST /api/manager/roads

Создание дороги.

**Тело запроса:**

| Параметр  | Тип    | Обязательный | Описание   |
|-----------|--------|--------------|------------|
| `roadName`| string?| нет          | Название   |

**Ответ:** 201, 404

```bash
curl -s -S -X POST "http://localhost/api/manager/roads" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"roadName\":\"Трасса М-7\"}"
```

---

## PUT /api/manager/roads/{id}

Обновление дороги. Тело — как в POST.

**Ответ:** 200, 404

```bash
curl -s -S -X PUT "http://localhost/api/manager/roads/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"roadName\":\"Трасса М-7 (обновлено)\"}"
```

---

## DELETE /api/manager/roads/{id}

Удаление дороги.

**Ответ:** 204, 404

```bash
curl -s -S -X DELETE "http://localhost/api/manager/roads/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
