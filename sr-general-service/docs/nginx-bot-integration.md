# Nginx + RoadCheck Integration

Документация по настройке Nginx для маршрутизации всех запросов к приложению.

## Обзор архитектуры

```
                    ┌─────────────────────────────────────┐
                    │          Nginx (порт 80)            │
                    │  - Reverse Proxy                    │
                    │  - Rate Limiting                    │
                    │  - Security Headers                 │
                    │  - SSL Termination (prod)           │
                    └──────────────┬──────────────────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
    ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
    │  Frontend   │         │  Bot API    │         │  Public API │
    │  (JWT/без)  │         │ (X-API-TOKEN)│        │  (JWT/без)  │
    └─────────────┘         └─────────────┘         └─────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────────┐
                    │    Spring Boot App (порт 8081)      │
                    │    (запущен в IDE на хост-машине)   │
                    └─────────────────────────────────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
    ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
    │  PostgreSQL │         │    Redis    │         │  Blockchain │
    │  (Docker)   │         │  (Docker)   │         │  (Docker)   │
    └─────────────┘         └─────────────┘         └─────────────┘
```

## Важные принципы

### 1. Backend доступен только через Nginx

- Spring Boot приложение **запускается на порту 8081**
- Nginx проксирует запросы на `host.docker.internal:8081`
- Nginx выступает как единая точка входа (API Gateway)

### 2. Типы аутентификации (актуальная схема)

| Тип | Заголовок | Endpoints | Описание |
|-----|-----------|-----------|----------|
| Без токена | — | `/api/auth/**`, `/api/regions/`, GET `/api/predictions/charts/**`, Swagger, `/docs` | Публичные эндпоинты (проверка в Spring) |
| JWT | `Authorization: Bearer <token>` | Остальной `/api/*` (кроме публичных выше) | Nginx **проксирует** заголовок `Authorization` в Spring; **решение 401/403** принимает Spring Security, а не nginx по regexp |
| X-API-TOKEN | `X-API-TOKEN: <секрет из BOT_API_TOKEN>` | `/api/internal/bot/*` | Только бот; проверка значения на стороне Spring |

Обычный пользовательский API за nginx **не** принимает только `X-API-TOKEN` без Bearer JWT (бот не подменяет пользователя).

### 3. Rate Limiting

| Зона | Лимит | Burst | Применение |
|------|-------|-------|------------|
| `static_limit` | 200 r/s | 100 | Фронтенд, статика |
| `api_limit` | 100 r/s | 50 | Публичное API |
| `bot_limit` | 50 r/s | 20 | Bot API |

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     Bot     │────▶│    Nginx    │────▶│  Spring App │
│  (VK Bot)   │     │  (Proxy)    │     │  (Backend)  │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ├────▶ PostgreSQL
                           ├────▶ Redis
                           └────▶ Blockchain
```

## Конфигурация Nginx

### Основные возможности

1. **Проверка X-API-TOKEN заголовка**
   - Блокировка запросов без токена
   - Валидация формата токена (regex: `^s\.[a-zA-Z0-9]{32}$`)

2. **Rate Limiting**
   - Bot API: 50 запросов/секунду (burst: 20)
   - Public API: 100 запросов/секунду (burst: 50)
   - Ограничение соединений: 10-20 на IP

3. **Логирование**
   - Отдельный лог для бот запросов (`/var/log/nginx/bot.log`)
   - Формат включает токен бота для аудита

4. **Security Headers**
   - X-Frame-Options
   - X-Content-Type-Options
   - X-XSS-Protection
   - Referrer-Policy

### Структура конфигурации

```
src/main/resources/nginx/
└── nginx.conf
```

### Ключевые директивы

#### 1. Карта валидации токена
```nginx
map $http_x_api_token $is_valid_bot_token {
    default                         0;
    "~^s\\.[a-zA-Z0-9]{32}$"        1;
}
```

#### 2. Bot API location
```nginx
location /api/internal/bot/ {
    # Проверка наличия заголовка
    if ($http_x_api_token = "") {
        return 401 '{"error": "X-API-TOKEN header is required"}\n';
    }

    # Проверка формата токена
    if ($is_valid_bot_token = 0) {
        return 401 '{"error": "Invalid X-API-TOKEN format"}\n';
    }

    # Rate limiting
    limit_req zone=bot_limit burst=20 nodelay;
    limit_conn conn_limit 10;

    # Логирование
    access_log /var/log/nginx/bot.log bot;

    # Проксирование
    proxy_pass http://backend;
    proxy_set_header X-Api-Token $http_x_api_token;
}
```

## Docker Compose

### Запуск локально

```bash
cd src/main/resources
docker-compose -f docker-compose-local.yml up -d
```

### Запуск production (единый стек)

В корне проекта:

```bash
# заполните значения в .env.production (HOST_IP, SERVER_NAME, пароли, JWT_SECRET, BOT_API_TOKEN)
docker compose -f docker-compose.prod.yml up -d --build
```

Что поднимается в production-стеке:

- `app` (сборка и запуск Spring Boot из `Dockerfile.prod`)
- `nginx` (reverse proxy на `:80`, конфиг из `nginx.prod.conf.template`)
- `db` (PostgreSQL)
- `redis` + `redis-ui` (Redis Commander)
- `ganache` (локальный blockchain RPC)
- `portainer` (управление контейнерами)

### Сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| nginx | 80 | Reverse proxy (проксирует на localhost:8081) |
| db | 5433 | PostgreSQL |
| redis | 6379 | Redis Cache |
| ganache | 8545 | Blockchain (test) |
| redis-ui | 8082 | Web UI для Redis (basic auth) |
| portainer | 9000 / 9443 | UI управления Docker |

### Запуск приложения

1. **Запустите инфраструктуру в Docker:**
   ```bash
   cd src/main/resources
   docker-compose -f docker-compose-local.yml up -d
   ```

2. **Запустите приложение в IDE:**
   - Откройте `RoadCheckApplication.kt`
   - Запустите с профилем `local`
   - Приложение должно слушать порт **8081**

3. **Проверьте работу:**
   ```bash
   curl http://localhost/health
   ```

### Переменные окружения

```bash
# В .env файле
BOT_API_TOKEN=<your-bot-token>

# В application-local.yml
server:
  port: 8081

bot:
  api-token: ${BOT_API_TOKEN:X-API-TOKEN}
```

## Примеры запросов

### Регистрация пользователя через бота

```bash
curl -X POST http://localhost/api/internal/bot/register \
  -H "X-API-TOKEN: <your-bot-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Создание отчёта

```bash
curl -X POST http://localhost/api/internal/bot/reports \
  -H "X-API-TOKEN: <your-bot-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Ошибки

#### Без токена
```bash
curl -X POST http://localhost/api/internal/bot/register \
  -H "Content-Type: application/json" \
  -d '{...}'
```
**Ответ:** `401 Unauthorized` - "X-API-TOKEN header is required"

#### С неверным токеном
```bash
curl -X POST http://localhost/api/internal/bot/register \
  -H "X-API-TOKEN: invalid-token" \
  -H "Content-Type: application/json" \
  -d '{...}'
```
**Ответ:** `401 Unauthorized` - "Invalid X-API-TOKEN format"

## Логи

### Формат логов бота

```
$remote_addr - [$time_local] "$request" 
$status $body_bytes_sent 
bot_token="$http_x_api_token" 
rt=$request_time
```

### Пример записи
```
192.168.1.100 - [16/Apr/2026:14:30:00 +0000] "POST /api/internal/bot/register HTTP/1.1" 
201 256 
bot_token="s-bot-x7k9m2" 
rt=0.045
```

## Production настройка

### 1. Измените токен
```bash
BOT_API_TOKEN=s-bot-$(openssl rand -hex 6)
```

### 2. Добавьте HTTPS
```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    
    # ... остальная конфигурация
}
```

### 3. Настройте upstream для кластера
```nginx
upstream backend {
    least_conn;
    server app1:8081 max_fails=3 fail_timeout=30s;
    server app2:8081 max_fails=3 fail_timeout=30s;
    server app3:8081 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

### 4. Увеличьте rate limits
```nginx
limit_req_zone $binary_remote_addr zone=bot_limit:10m rate=100r/s;
```

## Мониторинг

### Проверка статуса
```bash
curl http://localhost/health
```

### Просмотр логов бота
```bash
docker logs roadcheck-general-nginx-local --tail 100
docker exec roadcheck-general-nginx-local tail -f /var/log/nginx/bot.log
```

### Метрики Nginx
```bash
# Статистика запросов
awk '{print $9}' /var/log/nginx/bot.log | sort | uniq -c

# Среднее время ответа
awk '{sum+=$NF; count++} END {print sum/count}' /var/log/nginx/bot.log
```

## Безопасность

### Рекомендации

1. **Храните токен в секрете**
   - Используйте secrets manager (Vault, AWS Secrets Manager)
   - Не коммитьте токен в репозиторий

2. **Ограничьте доступ по IP**
   ```nginx
   location /api/internal/bot/ {
       allow 10.0.0.0/8;
       allow 172.16.0.0/12;
       deny all;
       # ...
   }
   ```

3. **Включите mutual TLS**
   - Клиентские сертификаты для бота
   - Двусторонняя аутентификация

4. **Аудит логов**
   - Регулярная проверка `/var/log/nginx/bot.log`
   - Alerting на подозрительную активность
