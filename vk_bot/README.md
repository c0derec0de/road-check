# RoadCheck VK Bot

VK-бот для приема дорожных обращений и проверки их статуса.

## Возможности

- регистрация обращения с телефоном, типом происшествия, временем, координатами и описанием;
- сохранение обращения в PostgreSQL;
- проверка статуса обращения по номеру;
- базовая валидация телефона, даты, координат и числовых полей.

## Запуск

```bash
python -m venv .venv
source .venv/bin/activate
pip install vk-api psycopg2-binary

export VK_GROUP_TOKEN=<token>
python postgrebot.py
```

Подключение к БД настраивается переменными `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. По умолчанию бот подключается к локальной базе `roadcheck` на порту `5433`.
