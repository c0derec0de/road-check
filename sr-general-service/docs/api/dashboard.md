# Дашборд

Требуется JWT (`Authorization: Bearer`).

- **MODERATOR** — метрики по всей системе (как раньше). Поле **`dangerousZones`** и недельная динамика зон (`zonesChange`) считаются только по **активным** опасным зонам (`is_active = true`); неактивные строки в БД в эти числа не входят.
- **USER** — только по своим обращениям: число инцидентов и динамика по вашим отчётам; «активные пользователи» условно **1**; опасные зоны и оценка риска — в **регионах, где у вас есть отчёты** (если отчётов нет — нули/пустые показатели). Число зон и его динамика за неделю также только по **активным** зонам в ваших регионах.

Пример `curl`:

```bash
curl -s -S "http://localhost/api/dashboard/metrics" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

## GET /api/dashboard/metrics

Общие метрики (см. различия ролей выше).

**Ответ:** 200, **401** без токена

| Поле              | Тип    | Описание                |
|-------------------|--------|-------------------------|
| `totalIncidents`  | long   | Всего инцидентов        |
| `incidentsChange` | string | Изменение за неделю     |
| `activeUsers`     | long   | Активных пользователей  |
| `usersChange`     | string | Изменение               |
| `safetyGrowth`    | double | Рост безопасности       |
| `safetyChange`    | string | Изменение               |
| `dangerousZones`  | long   | Число **активных** опасных зон (см. описание ролей выше) |
| `zonesChange`     | string | Изменение               |

---

## GET /api/dashboard/report-metrics

Метрики по отчётам. Для **USER** — только по своим отчётам.

**Ответ:** 200, **401**

| Поле               | Тип    | Описание               |
|--------------------|--------|------------------------|
| `total`            | long   | Всего отчётов          |
| `totalChange`      | string | Изменение              |
| `new`              | long   | Новых                  |
| `newChange`        | string | Изменение              |
| `inProgress`       | long   | В работе               |
| `inProgressChange` | string | Изменение              |
| `completed`        | long   | Завершённых            |
| `completedChange`  | string | Изменение              |

```bash
curl -s -S "http://localhost/api/dashboard/report-metrics" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

---

## GET /api/dashboard/current

Текущая погода и уровень риска (логика одна для всех ролей; требуется JWT для доступа к `/api/dashboard`).

**Query:** `lat` (number), `lng` (number) — координаты (по умолчанию Москва)

**Ответ:** 200, **401**

| Поле          | Тип    | Описание                                             |
|---------------|--------|------------------------------------------------------|
| `weather`     | object | condition, temperature, unit, windSpeedKmh, humidity |
| `riskLevel`   | string | Уровень риска                                        |
| `lastUpdated` | string | Время обновления                                     |

```bash
curl -s -S "http://localhost/api/dashboard/current?lat=55.75&lng=37.61" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```
