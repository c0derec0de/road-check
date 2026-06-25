# Аналитика

Требуется JWT.

Пример `curl`:

```bash
curl -s -S "http://localhost/api/analytics/dangerous-zones?regionId=1" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```

## GET /api/analytics/dangerous-zones

Список **только активных** опасных зон (`is_active = true`). Неактивные зоны не возвращаются.

- **MODERATOR** — все активные зоны; опционально `regionId`.
- **USER** — только зоны в **регионах, где у пользователя есть отчёты**. Параметр `regionId` учитывается, только если этот регион входит в «разрешённый» набор; иначе пустой список.

**Ответ:** 200, **401**

| Поле   | Тип   | Описание     |
|--------|-------|--------------|
| `zones`| array | Список зон   |
| `total`| int   | Количество   |

**Элемент zones:** id, name, incidents, riskLevel, coordinates (lat, lng)

```bash
curl -s -S "http://localhost/api/analytics/dangerous-zones" \
  -H "Authorization: Bearer YOUR_JWT_HERE"
```
