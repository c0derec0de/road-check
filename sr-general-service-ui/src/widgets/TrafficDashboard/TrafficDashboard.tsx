import {
  Badge,
  Box,
  Button,
  Card,
  Container,
  Divider,
  Grid,
  Group,
  Paper,
  Progress,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { IconRefresh } from "@tabler/icons-react";
import { SummaryStats } from "../SummaryStats/SummaryStats";
import { RiskMapSection } from "../RiskMapSection/RiskMapSection";
import {
  dangerousZonesApi,
  type DangerousZone,
} from "../../shared/api/dangerousZonesApi";
import { dashboardApi } from "../../shared/api/dashboardApi";
import { authUtils } from "../../shared/lib/auth";
import type { AreaRiskData, StatData } from "../../shared/types";

type YandexMetrika = (
  counterId: number,
  method: string,
  target: string,
) => void;

interface WindowWithMetrika extends Window {
  ym?: YandexMetrika;
}

const formatNumber = (value: number): string =>
  new Intl.NumberFormat("ru-RU").format(value);

const formatPercentValue = (value: number): string => {
  if (!Number.isFinite(value)) {
    return "-";
  }

  const rounded = value >= 10 ? value.toFixed(1) : value.toFixed(2);
  return `${rounded}%`;
};

const formatChangeValue = (value?: string): string => {
  if (!value) {
    return "без изменений";
  }

  const numeric = Number(String(value).replace("%", "").replace(",", "."));
  if (!Number.isFinite(numeric)) {
    return value;
  }

  const abs = Math.abs(numeric);
  const formatted = abs >= 10 ? abs.toFixed(0) : abs.toFixed(1);

  if (numeric > 0) {
    return `+${formatted}%`;
  }

  if (numeric < 0) {
    return `-${formatted}%`;
  }

  return "0%";
};

const getChangeType = (value?: string): "positive" | "negative" | "neutral" => {
  if (!value) return "neutral";
  if (value.includes("+")) return "positive";
  if (value.includes("-")) return "negative";
  return "neutral";
};

const riskLabel = (riskLevel?: string | null): string => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === "high") return "Высокий";
  if (normalized === "low") return "Низкий";
  if (normalized === "medium") return "Средний";
  return "Нет данных";
};

const riskColor = (riskLevel?: string | null): string => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === "high") return "red";
  if (normalized === "low") return "green";
  if (normalized === "medium") return "yellow";
  return "gray";
};

const formatWeatherLabel = (
  current: Awaited<ReturnType<typeof dashboardApi.getCurrent>> | null,
): string => {
  if (!current?.weather) {
    return "Данные о погоде недоступны";
  }

  const temperature =
    typeof current.weather.temperature === "number"
      ? `${current.weather.temperature}`
      : "температура недоступна";

  return `${"Температура"}, ${temperature}°C`;
};

const mapZoneToArea = (zone: DangerousZone): AreaRiskData => {
  const normalized = zone.riskLevel?.toLowerCase();
  const riskLevel =
    normalized === "high" ? "high" : normalized === "low" ? "low" : "medium";

  return {
    id: String(zone.id),
    name: zone.name,
    riskLevel,
    incidents: zone.incidents,
    coordinates: `${zone.coordinates.lat}, ${zone.coordinates.lng}`,
  };
};

export function TrafficDashboard() {
  const navigate = useNavigate();
  const isUser = authUtils.isUser();
  const isModerator = authUtils.isModerator();

  const dashboardQuery = useQuery({
    queryKey: ["traffic-dashboard"],
    queryFn: async () => {
      const [metrics, reportMetrics, current, zonesResponse] =
        await Promise.all([
          dashboardApi.getMetrics(),
          dashboardApi.getReportMetrics(),
          dashboardApi.getCurrent(),
          dangerousZonesApi.list(),
        ]);

      const areas = zonesResponse.zones
        .slice()
        .sort((a, b) => b.incidents - a.incidents)
        .slice(0, 5)
        .map(mapZoneToArea);

      const stats: StatData[] = [
        {
          title: "Инциденты",
          value: formatNumber(metrics.totalIncidents),
          change: formatChangeValue(metrics.incidentsChange),
          changeType: getChangeType(metrics.incidentsChange),
          iconType: "alert",
          hint: "динамика к прошлому периоду",
        },
        {
          title: "Активные пользователи",
          value: formatNumber(metrics.activeUsers),
          change: formatChangeValue(metrics.usersChange),
          changeType: getChangeType(metrics.usersChange),
          iconType: "users",
          hint: "пользователи с активностью",
        },
        {
          title: "Индекс безопасности",
          value: formatPercentValue(metrics.safetyGrowth),
          change: formatChangeValue(metrics.safetyChange),
          changeType: getChangeType(metrics.safetyChange),
          iconType: "trending",
          hint: "сводный показатель стабильности",
        },
        {
          title: "Опасные зоны",
          value: formatNumber(metrics.dangerousZones),
          change: formatChangeValue(metrics.zonesChange),
          changeType: getChangeType(metrics.zonesChange),
          iconType: "mapPin",
          hint: "активные зоны риска",
        },
      ];

      return {
        stats,
        current,
        areas,
        reportMetrics,
        loadedAt: new Date().toLocaleString("ru-RU"),
      };
    },
    refetchInterval: 60_000,
  });

  const stats = dashboardQuery.data?.stats ?? [];
  const current = dashboardQuery.data?.current ?? null;
  const areas = dashboardQuery.data?.areas ?? [];
  const reportMetrics = dashboardQuery.data?.reportMetrics ?? null;
  const loadedAt = dashboardQuery.data?.loadedAt ?? null;
  const isRefreshing = dashboardQuery.isFetching;

  const handleButtonClick = () => {
    dashboardQuery.refetch();
    const ym = (window as WindowWithMetrika).ym;
    if (ym) {
      ym(109023025, "reachGoal", "button_click");
    }
  };

  const reportStatusRows = useMemo(() => {
    if (!reportMetrics) {
      return [];
    }

    const totalBase = Math.max(
      reportMetrics.total,
      reportMetrics.new + reportMetrics.inProgress + reportMetrics.completed,
      1,
    );
    return [
      {
        label: "Новые",
        value: reportMetrics.new,
        change: formatChangeValue(reportMetrics.newChange),
        progress: (reportMetrics.new / totalBase) * 100,
        color: "blue",
      },
      {
        label: "В работе",
        value: reportMetrics.inProgress,
        change: formatChangeValue(reportMetrics.inProgressChange),
        progress: (reportMetrics.inProgress / totalBase) * 100,
        color: "gray",
      },
      {
        label: "Завершенные",
        value: reportMetrics.completed,
        change: formatChangeValue(reportMetrics.completedChange),
        progress: (reportMetrics.completed / totalBase) * 100,
        color: "green",
      },
    ];
  }, [reportMetrics]);

  return (
    <Container size="xl" className="app-page">
      <Stack gap="lg">
        <Paper p="lg" className="app-hero">
          <Group
            justify="space-between"
            align="flex-start"
            wrap="wrap"
            gap="lg"
          >
            <Box maw={760}>
              <Text className="app-kicker" mb={6}>
                Единая сводка
              </Text>
              <Title order={1} className="app-hero-title">
                {isModerator
                  ? "Операционный обзор дорожной обстановки"
                  : "Аналитика дорожной обстановки"}
              </Title>
              <Text mt={8} className="app-hero-copy">
                Контроль отчетов, зон риска и текущих показателей без лишних
                декоративных блоков.
              </Text>
            </Box>
            <Stack gap={6} align="flex-end">
              <Badge
                size="lg"
                variant="light"
                color={isRefreshing ? "gray" : "blue"}
              >
                {isRefreshing ? "Обновление" : "Данные актуальны"}
              </Badge>
              <Button
                size="xs"
                variant="light"
                leftSection={<IconRefresh size={16} />}
                loading={isRefreshing}
                onClick={handleButtonClick}
              >
                Обновить данные
              </Button>
              <Text size="xs" c="dimmed">
                Обновлено: {loadedAt || "-"}
              </Text>
            </Stack>
          </Group>
        </Paper>

        <SummaryStats stats={stats} />

        <Grid gutter="lg">
          <Grid.Col span={{ base: 12, lg: 8 }}>
            <Paper p="lg" className="app-surface">
              <Group justify="space-between" align="flex-start" mb="md">
                <Box>
                  <Title order={2} className="app-panel-title">
                    Очередь отчетов
                  </Title>
                  <Text size="sm" c="dimmed" mt={4}>
                    Состояние обработки обращений за текущий период.
                  </Text>
                </Box>
                <Button variant="subtle" onClick={() => navigate("/appeals")}>
                  Открыть отчеты
                </Button>
              </Group>

              {reportMetrics ? (
                <Stack gap="md">
                  <Group justify="space-between">
                    <Text size="sm" c="dimmed">
                      Всего отчетов
                    </Text>
                    <Text fw={700} size="lg">
                      {formatNumber(reportMetrics.total)}
                    </Text>
                  </Group>
                  <Divider />
                  {reportStatusRows.map((row) => (
                    <Box key={row.label}>
                      <Group justify="space-between" mb={6}>
                        <Text size="sm" fw={600}>
                          {row.label}
                        </Text>
                        <Group gap="xs">
                          <Text size="sm" fw={700}>
                            {formatNumber(row.value)}
                          </Text>
                          <Text size="xs" c="dimmed">
                            {row.change}
                          </Text>
                        </Group>
                      </Group>
                      <Progress value={row.progress} color={row.color} />
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Text size="sm" c="dimmed">
                  Данные загружаются.
                </Text>
              )}
            </Paper>
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 4 }}>
            <Stack gap="lg">
              <Card className="app-surface" p="lg">
                <Title order={2} className="app-panel-title">
                  Текущие условия
                </Title>
                <Text size="sm" c="dimmed" mt={8}>
                  {formatWeatherLabel(current)}
                </Text>
                <Group justify="space-between" mt="md">
                  <Text size="sm" c="dimmed">
                    Уровень риска
                  </Text>
                  <Badge color={riskColor(current?.riskLevel)} variant="light">
                    {riskLabel(current?.riskLevel)}
                  </Badge>
                </Group>
              </Card>
            </Stack>
          </Grid.Col>
        </Grid>

        {!isUser ? (
          <Paper p="lg" className="app-surface">
            <Group justify="space-between" align="flex-start" mb="md">
              <Box>
                <Title order={2} className="app-panel-title">
                  Участки повышенного внимания
                </Title>
                <Text size="sm" c="dimmed" mt={4}>
                  Пять зон с наибольшим числом зарегистрированных инцидентов.
                </Text>
              </Box>
              <Button
                variant="subtle"
                onClick={() => navigate("/dangerous-zones")}
              >
                Управлять
              </Button>
            </Group>
            <SimpleGrid cols={{ base: 1, md: 5 }} spacing="sm">
              {areas.map((area) => (
                <Card key={area.id} withBorder p="md">
                  <Text fw={700} lineClamp={2}>
                    {area.name}
                  </Text>
                  <Text size="xs" c="dimmed" mt={6}>
                    {area.coordinates}
                  </Text>
                  <Group justify="space-between" mt="md">
                    <Badge color={riskColor(area.riskLevel)} variant="light">
                      {riskLabel(area.riskLevel)}
                    </Badge>
                    <Text size="sm" fw={700}>
                      {area.incidents}
                    </Text>
                  </Group>
                </Card>
              ))}
            </SimpleGrid>
          </Paper>
        ) : null}

        <RiskMapSection current={current} areas={areas} showTrends={!isUser} />
      </Stack>
    </Container>
  );
}
