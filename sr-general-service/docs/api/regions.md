# Регионы (публичный API)

Базовый путь: `/api/regions`

Пример `curl`:

```bash
curl -s -S "http://localhost/api/regions"
```

## GET /api/regions

Список всех регионов с координатами центра.

**Ответ:** 200 — массив объектов `RegionResponse`

| Поле        | Тип        | Описание                 |
|-------------|------------|--------------------------|
| `id`        | long       | ID региона               |
| `regCode`   | string     | Код региона              |
| `regName`   | string     | Название региона         |
| `centerLat` | number     | Широта центра региона    |
| `centerLng` | number     | Долгота центра региона   |

---

## GET /api/regions/{id}

Регион по идентификатору.

**Ответы:**

- 200 — объект `RegionResponse`
- 404 — регион не найден

```bash
curl -s -S "http://localhost/api/regions/1"
```

---

## GET /api/regions/{id}/weather

Погода в указанном регионе «сейчас».

Сервис берёт координаты центра региона и запрашивает внешнее API погоды (Open-Meteo, `https://open-meteo.com/`) с кэшированием результата.

**Ответы:**

- 200 — объект `RegionWeatherResponse`
- 404 — регион не найден, не заданы координаты центра или не удалось получить данные погоды

**Ответ 200:**

| Поле             | Тип    | Описание                                         |
|------------------|--------|--------------------------------------------------|
| `regionId`       | long   | ID региона                                       |
| `regionName`     | string | Название региона                                 |
| `temperature`    | int    | Температура, °C                                  |
| `precipitationMm`| number | Осадки за последний период, мм (если есть)      |
| `windSpeedKmh`   | number | Скорость ветра, км/ч                            |
| `humidity`       | int    | Влажность воздуха, %                            |

```bash
curl -s -S "http://localhost/api/regions/1/weather"
```

