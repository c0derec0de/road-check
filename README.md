# Road Check

Road Check — платформа мониторинга для сбора и анализа информации о дорожных происшествиях. В нем есть backend, веб-интерфейс для сотрудников и VK-бот, через который пользователь может отправить обращение.

## Состав проекта

| Папка | Что внутри |
| --- | --- |
| `sr-general-service` | Backend на Kotlin + Spring Boot. Хранит данные, отдает REST API, считает аналитику и работает с обращениями. |
| `sr-general-service-ui` | Frontend на React + TypeScript. Интерфейс для просмотра обращений, карт, регионов, дорог, пользователей и опасных зон. |
| `vk_bot` | VK-бот на Python. Принимает обращения от пользователей и сохраняет их в базу. |

## Что реализовано

- регистрация, авторизация и роли пользователей;
- работа с обращениями о дорожных происшествиях;
- страницы для аналитики, дашборда, регионов, дорог, пользователей и опасных зон;
- карта рисков и прогнозирование опасных участков;
- REST API для frontend и интеграции с ботом;
- VK-бот с пошаговым сбором данных: телефон, тип события, дата, координаты, описание и адрес;
- хранение данных в PostgreSQL, миграции через Liquibase, кэширование через Redis;

## Технологии

- Kotlin, Spring Boot, Gradle;
- PostgreSQL, Redis, Liquibase;
- React, TypeScript, Vite;
- Python, `vk-api`, `psycopg2`;
- Docker и Docker Compose.

## Запуск

### Backend

```bash
cd sr-general-service
cp .env.example .env
./gradlew localBootRun
```

Backend запускается на `http://localhost:8081`.

### Frontend

```bash
cd sr-general-service-ui
cp .env.example .env
npm install
npm run dev
```

Frontend запускается на `http://localhost:5173`.

### VK-бот

```bash
cd vk_bot
cp .env.example .env
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python postgrebot.py
```

## Документация API

После запуска backend доступен Swagger:
```text
http://localhost:8081/swagger-ui.html
```
Дополнительные документы лежат в `sr-general-service/docs`.