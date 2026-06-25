# Управление опасными зонами

Базовый путь: `/api/manager/dangerous-zones`

**Авторизация:** JWT, роль **MODERATOR**.

## GET /api/manager/dangerous-zones

Список **только активных** опасных зон (`is_active = true`). Неактивные зоны в общий список не попадают; по **id** зона по-прежнему доступна в `GET /{id}` и в `PUT` для правки или повторной активации.

**Ответ:** 200 — массив DangerousZoneManagerResponse

```bash
curl -s -S "http://localhost/api/manager/dangerous-zones" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/manager/dangerous-zones/{id}

Опасная зона по id.

**Ответ:** 200, 404

```bash
curl -s -S "http://localhost/api/manager/dangerous-zones/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## POST /api/manager/dangerous-zones

Создание опасной зоны.

**Тело запроса:**

| Параметр       | Тип     | Обязательный | Описание      |
|----------------|---------|--------------|---------------|
| `name`         | string? | нет          | Название      |
| `centerLat`    | number? | нет          | Широта центра |
| `centerLng`    | number? | нет          | Долгота центра|
| `radius`       | int?    | нет          | Радиус (м)    |
| `incidentsCount`| int?   | нет          | Кол-во инцидентов |
| `riskLevel`    | string? | нет          | high/medium/low |
| `isActive`     | bool?   | нет          | Активна       |

**Ответ:** 201, 404

```bash
curl -s -S -X POST "http://localhost/api/manager/dangerous-zones" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Опасный перекресток\",\"centerLat\":55.75,\"centerLng\":37.61,\"radius\":250,\"riskLevel\":\"high\",\"isActive\":true}"
```

---

## PUT /api/manager/dangerous-zones/{id}

Обновление опасной зоны. Тело — как в POST.

**Ответ:** 200, 404

```bash
curl -s -S -X PUT "http://localhost/api/manager/dangerous-zones/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Опасный перекресток (обновлено)\",\"isActive\":true}"
```

---

## DELETE /api/manager/dangerous-zones/{id}

Удаление опасной зоны.

**Ответ:** 204, 404

```bash
curl -s -S -X DELETE "http://localhost/api/manager/dangerous-zones/1" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```
