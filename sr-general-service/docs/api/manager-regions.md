# Управление регионами

Базовый путь: `/api/manager/regions`

**Авторизация:** JWT, роль **MODERATOR**.

## GET /api/manager/regions

Список регионов.

**Ответ:** 200 — массив RegionManagerResponse

```bash
curl -s -S "http://localhost/api/manager/regions" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/manager/regions/{id}

Регион по id.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/manager/regions/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## POST /api/manager/regions

Создание региона.

**Тело запроса:**

| Параметр | Тип    | Обязательный | Описание   |
|----------|--------|--------------|------------|
| `regCode`| string | да           | Код региона|
| `regName`| string | да           | Название   |

**Ответ:** 201, 400, 404

```bash
curl -s -S -X POST "http://localhost/api/manager/regions" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"regCode\":\"77\",\"regName\":\"Москва\"}"
```

---

## PUT /api/manager/regions/{id}

Обновление региона. Тело — как в POST.

**Ответ:** 200, 404

```bash
curl -s -S -X PUT "http://localhost/api/manager/regions/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"regCode\":\"77\",\"regName\":\"Москва (обновлено)\"}"
```

---

## DELETE /api/manager/regions/{id}

Удаление региона.

**Ответ:** 204, 404

```bash
curl -s -S -X DELETE "http://localhost/api/manager/regions/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
