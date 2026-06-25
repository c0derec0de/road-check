import {
  Badge,
  Box,
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
  ThemeIcon,
  Title,
} from "@mantine/core";
import { useEffect, useMemo, useState } from "react";
import {
  IconAlertTriangle,
  IconClipboardList,
  IconMapPin,
  IconRoute,
  IconShieldCheck,
} from "@tabler/icons-react";
import {
  dangerousZonesApi,
  type DangerousZone,
} from "../../shared/api/dangerousZonesApi";

const getRiskLevel = (risk?: string | null): "high" | "medium" | "low" => {
  const normalized = risk?.toLowerCase();
  if (normalized === "high") return "high";
  if (normalized === "low") return "low";
  return "medium";
};

const getRiskLabel = (risk: "high" | "medium" | "low"): string => {
  if (risk === "high") return "Высокий риск";
  if (risk === "medium") return "Средний риск";
  return "Низкий риск";
};

const getRiskColor = (risk: "high" | "medium" | "low"): string => {
  if (risk === "high") return "red";
  if (risk === "medium") return "green";
  return "green";
};

const getPatrolWindow = (risk: "high" | "medium" | "low"): string => {
  if (risk === "high") return "07:00-10:00";
  if (risk === "medium") return "12:00-15:00";
  return "17:00-20:00";
};

const getRiskScore = (
  risk: "high" | "medium" | "low",
  incidents: number,
): number => {
  const base = risk === "high" ? 70 : risk === "medium" ? 45 : 25;
  return Math.min(99, base + Math.min(25, incidents));
};

const buildRecommendation = (zone: DangerousZone): string => {
  const risk = getRiskLevel(zone.riskLevel);
  const name = zone.name || `Зона #${zone.id}`;

  if (risk === "high") {
    return `Усилить патруль в районе «${name}»: высокий риск и ${zone.incidents} инцидентов.`;
  }
  if (risk === "medium") {
    return `Назначить профилактический патруль для «${name}»: средний риск и ${zone.incidents} инцидентов.`;
  }
  return `Оставить плановый контроль для «${name}»: низкий риск и ${zone.incidents} инцидентов.`;
};

export function AIAssistantPage() {
  const [zones, setZones] = useState<DangerousZone[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await dangerousZonesApi.list();
        const sorted = response.zones
          .slice()
          .sort((a, b) => (b.incidents ?? 0) - (a.incidents ?? 0));
        setZones(sorted);
      } catch {
        setZones([]);
        setError("Не удалось загрузить данные для планирования");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, []);

  const summary = useMemo(() => {
    const totalZones = zones.length;
    const totalIncidents = zones.reduce(
      (acc, zone) => acc + (zone.incidents ?? 0),
      0,
    );
    const highRiskCount = zones.filter(
      (zone) => getRiskLevel(zone.riskLevel) === "high",
    ).length;
    const mediumRiskCount = zones.filter(
      (zone) => getRiskLevel(zone.riskLevel) === "medium",
    ).length;
    const lowRiskCount = zones.filter(
      (zone) => getRiskLevel(zone.riskLevel) === "low",
    ).length;
    const topZone = zones[0];

    return {
      totalZones,
      totalIncidents,
      highRiskCount,
      mediumRiskCount,
      lowRiskCount,
      topZoneName:
        topZone?.name || (topZone ? `Зона #${topZone.id}` : "Нет данных"),
      topZoneIncidents: topZone?.incidents ?? 0,
    };
  }, [zones]);

  const topZones = zones.slice(0, 5);
  const recommendations = topZones.map(buildRecommendation);

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper
          p="md"
          className="app-hero"
        >
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Box maw={780}>
              <Group gap="sm" mb="sm">
                <ThemeIcon size={32} radius="sm" variant="light" color="blue">
                  <IconClipboardList size={18} />
                </ThemeIcon>
                <Title order={1} className="app-hero-title">Оперативное планирование</Title>
              </Group>
              <Text className="app-hero-copy">
                Оперативная сводка для инспектора: приоритетные зоны, оценка
                риска и рекомендованные действия на текущую смену.
              </Text>
            </Box>

            <Badge size="lg" color={error ? "red" : "teal"} variant="light">
              {isLoading
                ? "Обновление..."
                : error
                  ? "Есть проблемы с данными"
                  : "Данные актуальны"}
            </Badge>
          </Group>
        </Paper>

        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing="md">
          <Card className="app-surface" p="md">
            <Group justify="space-between">
              <Text c="dimmed" size="sm">
                Опасные зоны
              </Text>
              <ThemeIcon color="blue" variant="light" size="sm">
                <IconMapPin size={16} />
              </ThemeIcon>
            </Group>
            <Title order={2} mt="sm">
              {summary.totalZones}
            </Title>
            <Text size="xs" c="dimmed" mt={4}>
              Зон в мониторинге
            </Text>
          </Card>

          <Card className="app-surface" p="md">
            <Group justify="space-between">
              <Text c="dimmed" size="sm">
                Всего инцидентов
              </Text>
              <ThemeIcon color="red" variant="light" size="sm">
                <IconAlertTriangle size={16} />
              </ThemeIcon>
            </Group>
            <Title order={2} mt="sm">
              {summary.totalIncidents}
            </Title>
            <Text size="xs" c="dimmed" mt={4}>
              По всем зонам
            </Text>
          </Card>

          <Card className="app-surface" p="md">
            <Group justify="space-between">
              <Text c="dimmed" size="sm">
                Зоны высокого риска
              </Text>
              <ThemeIcon color="orange" variant="light" size="sm">
                <IconShieldCheck size={16} />
              </ThemeIcon>
            </Group>
            <Title order={2} mt="sm">
              {summary.highRiskCount}
            </Title>
            <Text size="xs" c="dimmed" mt={4}>
              Требуют усиленного патруля
            </Text>
          </Card>

          <Card className="app-surface" p="md">
            <Group justify="space-between">
              <Text c="dimmed" size="sm">
                Самая критичная зона
              </Text>
              <ThemeIcon color="blue" variant="light" size="sm">
                <IconRoute size={16} />
              </ThemeIcon>
            </Group>
            <Text fw={700} mt="sm" lineClamp={1}>
              {summary.topZoneName}
            </Text>
            <Text size="xs" c="dimmed" mt={4}>
              Инцидентов: {summary.topZoneIncidents}
            </Text>
          </Card>
        </SimpleGrid>

        <Grid gutter="md">
          <Grid.Col span={{ base: 12, lg: 7 }}>
            <Paper className="app-surface" p="md">
              <Group justify="space-between" mb="md">
                <Title order={3}>Приоритетные зоны</Title>
                <Badge color="red" variant="light">
                  Топ-{topZones.length}
                </Badge>
              </Group>

              {isLoading && <Text c="dimmed">Загрузка зон...</Text>}
              {!isLoading && topZones.length === 0 && (
                <Text c="dimmed">Нет данных по зонам</Text>
              )}

              <Stack gap="md">
                {topZones.map((zone) => {
                  const risk = getRiskLevel(zone.riskLevel);
                  const score = getRiskScore(risk, zone.incidents ?? 0);

                  return (
                    <Paper key={zone.id} p="sm" className="app-surface">
                      <Group justify="space-between" align="flex-start">
                        <Box style={{ flex: 1 }}>
                          <Text fw={600}>
                            {zone.name || `Зона #${zone.id}`}
                          </Text>
                          <Text size="sm" c="dimmed">
                            Координаты: {zone.coordinates.lat},{" "}
                            {zone.coordinates.lng}
                          </Text>
                        </Box>
                        <Badge color={getRiskColor(risk)} variant="light">
                          {getRiskLabel(risk)}
                        </Badge>
                      </Group>
                      <Group justify="space-between" mt="sm" mb={6}>
                        <Text size="sm">Инцидентов: {zone.incidents ?? 0}</Text>
                        <Text size="sm" fw={600}>
                          Индекс риска: {score}%
                        </Text>
                      </Group>
                      <Progress
                        value={score}
                        color={getRiskColor(risk)}
                        radius="sm"
                      />
                    </Paper>
                  );
                })}
              </Stack>
            </Paper>
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 5 }}>
            <Paper className="app-surface" p="md" h="100%">
              <Title order={3} mb="md">
                План действий
              </Title>
              <Stack gap="sm">
                {recommendations.length === 0 && (
                  <Text c="dimmed">Рекомендации недоступны</Text>
                )}
                {recommendations.map((text, index) => (
                  <Paper
                    key={index}
                    p="sm"
                    radius="md"
                    className="app-surface"
                  >
                    <Text size="sm">
                      {index + 1}. {text}
                    </Text>
                  </Paper>
                ))}
              </Stack>

              <Divider my="md" />

              <Text size="sm" c="dimmed" mb={8}>
                Распределение зон по риску
              </Text>
              <Stack gap={8}>
                <Group justify="space-between">
                  <Text size="sm">Высокий</Text>
                  <Text size="sm" fw={600}>
                    {summary.highRiskCount}
                  </Text>
                </Group>
                <Progress
                  value={
                    summary.totalZones
                      ? (summary.highRiskCount / summary.totalZones) * 100
                      : 0
                  }
                  color="red"
                />
                <Group justify="space-between">
                  <Text size="sm">Средний</Text>
                  <Text size="sm" fw={600}>
                    {summary.mediumRiskCount}
                  </Text>
                </Group>
                <Progress
                  value={
                    summary.totalZones
                      ? (summary.mediumRiskCount / summary.totalZones) * 100
                      : 0
                  }
                  color="green"
                />
                <Group justify="space-between">
                  <Text size="sm">Низкий</Text>
                  <Text size="sm" fw={600}>
                    {summary.lowRiskCount}
                  </Text>
                </Group>
                <Progress
                  value={
                    summary.totalZones
                      ? (summary.lowRiskCount / summary.totalZones) * 100
                      : 0
                  }
                  color="green"
                />
              </Stack>
            </Paper>
          </Grid.Col>
        </Grid>

        <Paper className="app-surface" p="md">
          <Group justify="space-between" mb="md">
            <Title order={3}>Рекомендуемые окна патрулирования</Title>
            <Badge color="blue" variant="light">
              На сегодня
            </Badge>
          </Group>

          <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
            {topZones.map((zone) => {
              const risk = getRiskLevel(zone.riskLevel);
              return (
                <Card key={`patrol-${zone.id}`} className="app-surface" p="md">
                  <Text fw={600} lineClamp={1}>
                    {zone.name || `Зона #${zone.id}`}
                  </Text>
                  <Text size="sm" c="dimmed" mt={4}>
                    Время патруля: {getPatrolWindow(risk)}
                  </Text>
                  <Text size="sm" mt={8}>
                    Причина: {zone.incidents ?? 0} инцидентов
                  </Text>
                  <Badge mt="sm" color={getRiskColor(risk)} variant="light">
                    {getRiskLabel(risk)}
                  </Badge>
                </Card>
              );
            })}
          </SimpleGrid>
        </Paper>
      </Stack>
    </Container>
  );
}
