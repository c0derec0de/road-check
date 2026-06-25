# Production deployment (Docker Compose)

Файл: `docker-compose.prod.yml`

## Что входит в стек

- `app` — сборка и запуск сервиса (`Dockerfile.prod`)
- `nginx` — reverse proxy на `80`, проксирование в `app:8081`
- `db` — PostgreSQL 15
- `redis` — Redis 7
- `redis-ui` — Redis Commander
- `ganache` — blockchain RPC для интеграции
- `portainer` — web UI для управления контейнерами

## Подготовка `.env`

Отредактируйте файл `.env.production` в корне проекта.

Обязательные поля для редактирования:

- `HOST_IP` — IP вашего сервера
- `SERVER_NAME` — домен/хост (или `localhost`)
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `BOT_API_TOKEN`
- `REDIS_UI_PASSWORD`

## Запуск

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## Проверка

```bash
curl -s -S "http://<HOST_IP>/health"
```

```bash
curl -s -S "http://<HOST_IP>/swagger-ui.html"
```

## Полезные URL

- API / Swagger: `http://<HOST_IP>/swagger-ui.html`
- Docs: `http://<HOST_IP>/docs`
- Portainer: `http://<HOST_IP>:9000`
- Redis UI: `http://<HOST_IP>:8082`

> В профиле `prod` swagger server URL также формируется по `HOST_IP`.
