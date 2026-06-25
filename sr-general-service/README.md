# RoadCheck Backend

Основной backend-сервис RoadCheck на Kotlin и Spring Boot.

## Реализовано

- JWT-авторизация и роли `USER`, `MODERATOR`, `BOT`;
- REST API для обращений, регионов, дорог, пользователей и опасных зон;
- аналитика, метрики дашборда и SVG-графики прогнозов;
- запуск задач пересчета опасных зон и рисков;
- интеграция с внешним погодным API;
- Redis-кэш, Liquibase-миграции и PostgreSQL;
- внутренний API для VK-бота;
- Swagger/OpenAPI и встроенная HTML-документация.

## Требования

* Java 21
* Docker
* Gradle Wrapper из репозитория

## Локальный запуск

```bash
./gradlew localBootRun
```

Команда поднимает Postgres, Redis, nginx и Ganache из `src/main/resources/docker-compose-local.yml`, затем запускает приложение с профилем `local`.

Прямой адрес backend: `http://localhost:8081`.
Адрес через nginx: `http://localhost`.

## Документация

REST API: [docs/api/README.md](docs/api/README.md).
Swagger UI после запуска: `http://localhost/swagger-ui/index.html`.
Документация проекта: `http://localhost/docs`.

## Проверка

```bash
./gradlew test
./gradlew detekt
```
