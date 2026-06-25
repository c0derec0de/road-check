# Планировщик задач (Quartz Jobs)

Планировщик Quartz используется для выполнения периодических фоновых задач в приложении RoadCheck.

## Обзор

Все задачи конфигурируются в классе `JobConfig` и настраиваются через свойства `JobsProperties`.

---

## Задачи

### PredictionJob

**Назначение**: Выполнение Python-скриптов для ML-предсказаний рисков и перерасчёта опасных зон.

**Расположение**: `src/main/kotlin/ru/cs/roadcheck/job/PredictionJob.kt`

**Периодичность**: Каждые N минут (настраивается через `jobs.properties.predictions.intervalMinutes`)

**Алгоритм работы**:
1. **Перерасчёт опасных зон** — запуск скрипта `dangerous_code.py` для обновления данных об опасных участках дорог (строки, не попавшие в новый расчёт, помечаются `is_active = false`; актуальные зоны — `true`. Неактивные зоны не участвуют в дашбордах, аналитике и графиках риска в API.)
2. **Предсказание рисков** — запуск скрипта `risk.py` для расчёта прогнозов рисков для дорог

**Обработка ошибок**:
- При неудачном выполнении перерасчёта опасных зон, предсказание рисков пропускается
- Все ошибки логируются через SLF4J
- При прерывании выполнения восстанавливается статус прерывания потока

**Свойства конфигурации**:
```yaml
jobs:
  predictions:
    intervalMinutes: 60  # Интервал запуска предсказаний (в минутах)
    script:
      path: /path/to/risk.py  # Путь к основному Python-скрипту
```

**Связанные файлы**:
- `src/main/resources/scripts/risk.py` — основной скрипт предсказания рисков
- `src/main/resources/scripts/dangerous_code.py` — скрипт расчёта опасных зон

---

### ArchiveReportsJob

**Назначение**: Архивация обработанных отчётов для поддержания производительности базы данных.

**Расположение**: `src/main/kotlin/ru/cs/roadcheck/job/ArchiveReportsJob.kt`

**Периодичность**: По cron-расписанию (настраивается через `jobs.properties.reports.archiveCron`)

**Алгоритм работы**:
1. Вызов метода `ReportService.archiveProcessedReports()` для архивации всех обработанных отчётов
2. Логирование количества заархивированных отчётов

**Свойства конфигурации**:
```yaml
jobs:
  reports:
    archiveCron: "0 0 2 * * ?"  # Ежедневно в 2:00 (пример)
```

**Связанные сервисы**:
- `ReportService.archiveProcessedReports()` — сервисный метод архивации отчётов

---

## Конфигурация

### JobsProperties

Все свойства задач настраиваются через класс `JobsProperties`:

```kotlin
@ConfigurationProperties("jobs")
class JobsProperties {
    val predictions: PredictionsProperties
    val reports: ReportsProperties
}
```

### Пример application.yml

```yaml
jobs:
  predictions:
    intervalMinutes: 60
    script:
      path: /app/scripts/risk.py
  reports:
    archiveCron: "0 0 2 * * ?"
```

## Идентификаторы задач

| Задача | Identity | Группа |
|--------|----------|--------|
| PredictionJob | `predictionJob` | `predictionGroup` |
| ArchiveReportsJob | `archiveReportsJob` | `reportsGroup` |

| Триггер | Identity | Группа |
|---------|----------|--------|
| Prediction Trigger | `predictionTrigger` | `predictionGroup` |
| Archive Reports Trigger | `archiveReportsTrigger` | `reportsGroup` |

## Добавление новых задач

Для добавления новой запланированной задачи:

1. Создайте класс задачи, реализующий `org.quartz.Job` в `src/main/kotlin/ru/cs/roadcheck/job/`
2. Добавьте bean-методы для job detail и триггера в `JobConfig.kt`
3. При необходимости добавьте свойства конфигурации в `JobsProperties.kt`
4. Задокументируйте задачу в этом файле

Пример:
```kotlin
@Component
class MyNewJob : Job {
    override fun execute(context: JobExecutionContext?) {
        // Логика задачи
    }
}
```

```kotlin
@Bean
fun myNewJobDetail(): JobDetail {
    return JobBuilder.newJob(MyNewJob::class.java)
        .withIdentity("myNewJob", "myGroup")
        .storeDurably()
        .build()
}

@Bean
fun myNewJobTrigger(): Trigger {
    return TriggerBuilder.newTrigger()
        .forJob(myNewJobDetail())
        .withIdentity("myNewTrigger", "myGroup")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
        .build()
}
```

## Мониторинг

Логи выполнения задач доступны через SLF4J. Ключевые сообщения:
- `Python script ($description) executed successfully` — задача предсказания выполнена успешно
- `Python script ($description) failed with exit code $exitCode` — задача предсказания не выполнена
- `Archive reports job completed. Archived reports: {}` — задача архивации завершена
