# Предсказания и графики

Базовый путь: `/api/predictions`

Доступ:

- `POST /api/predictions/run-manual` — только **MODERATOR**.
- `GET /api/predictions/charts/**` — доступно всем (без JWT).

## POST /api/predictions/run-manual

Ручной запуск пайплайна предсказаний (сначала пересчёт опасных зон, затем пересчёт рисков).

**Тело запроса:** не требуется  
**Ответ:** 200 (строка), 500 (строка с ошибкой), 403 (не MODERATOR)

Пример `curl`:

```bash
curl -s -S -X POST "http://localhost/api/predictions/run-manual" \
  -H "Authorization: Bearer MODERATOR_JWT_HERE"
```

---

## GET /api/predictions/charts/monthly

SVG-график ДТП по месяцам.

**Ответ:** 200  
**Content-Type:** `image/svg+xml`

Пример `curl`:

```bash
curl -s -S "http://localhost/api/predictions/charts/monthly"
```

---

## GET /api/predictions/charts/risk

SVG-график распределения рисков. В расчёт попадают **только активные** опасные зоны (как в аналитике по зонам); неактивные не учитываются.

**Ответ:** 200  
**Content-Type:** `image/svg+xml`

Пример `curl`:

```bash
curl -s -S "http://localhost/api/predictions/charts/risk"
```

---

## GET /api/predictions/charts/causes

SVG-график распределения причин ДТП.

**Ответ:** 200  
**Content-Type:** `image/svg+xml`

Пример `curl`:

```bash
curl -s -S "http://localhost/api/predictions/charts/causes"
```

