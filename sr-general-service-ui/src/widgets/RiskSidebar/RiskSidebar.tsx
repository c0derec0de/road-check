import { Card, Stack, Title, Text, Group, Badge, Divider } from "@mantine/core";
import type { CurrentWeatherResponse } from "../../shared/api/dashboardApi";

interface RiskSidebarProps {
  current: CurrentWeatherResponse | null;
}

const getRiskBadgeColor = (riskLevel?: string): string => {
  if (!riskLevel) return "gray";
  const normalized = riskLevel.toLowerCase();
  if (normalized.includes("high") || normalized.includes("выс")) return "red";
  if (normalized.includes("medium") || normalized.includes("сред"))
    return "orange";
  if (normalized.includes("low") || normalized.includes("низ")) return "green";
  return "blue";
};

export function RiskSidebar({ current }: RiskSidebarProps) {
  return (
    <Card padding="md" className="app-surface" h="100%">
      <Stack gap="md">
        <div>
          <Title order={4} fw={600} mb="xs">
            Текущие условия
          </Title>
          <Text size="sm" c="dimmed">
            {current
              ? `${current.weather.temperature}°C`
              : "Данные о погоде недоступны"}
          </Text>
          <Group gap="xs" mt="xs">
            <Text size="sm">Уровень риска:</Text>
            <Badge
              color={getRiskBadgeColor(current?.riskLevel)}
              variant="light"
            >
              {current?.riskLevel ?? "Нет данных"}
            </Badge>
          </Group>
          {current?.lastUpdated && (
            <Text size="xs" c="dimmed" mt={4}>
              Обновлено: {current.lastUpdated}
            </Text>
          )}
        </div>

        <Divider />

        <Title order={4} fw={600}>
          Легенда рисков
        </Title>
        <Stack gap="sm">
          <Group gap="xs">
            <Badge color="red" size="lg" variant="filled" />
            <Text size="sm">Высокий риск</Text>
          </Group>
          <Group gap="xs">
            <Badge color="orange" size="lg" variant="filled" />
            <Text size="sm">Средний риск</Text>
          </Group>
          <Group gap="xs">
            <Badge color="green" size="lg" variant="filled" />
            <Text size="sm">Низкий риск</Text>
          </Group>
        </Stack>

        <Divider />

        <Text size="xs" c="dimmed">
          Тепловая карта показывает концентрацию инцидентов на участках дорожной
          сети. Чем «теплее» зона, тем выше приоритет патрулирования.
        </Text>
      </Stack>
    </Card>
  );
}
