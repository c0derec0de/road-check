import { Group, Text, Badge, Paper, Progress } from "@mantine/core";
import type { AreaRiskData } from "../../shared/types";

interface DangerousAreaItemProps {
  area: AreaRiskData;
}

export function DangerousAreaItem({ area }: DangerousAreaItemProps) {
  const badgeColor =
    area.riskLevel === "high"
      ? "red"
      : area.riskLevel === "medium"
        ? "orange"
        : "green";
  const label =
    area.riskLevel === "high"
      ? "Высокий"
      : area.riskLevel === "medium"
        ? "Средний"
        : "Низкий";
  const trendValue = Math.min(100, Math.max(10, area.incidents * 5));

  return (
    <Paper p="md" radius="md" withBorder>
      <Group justify="space-between" align="flex-start">
        <div style={{ flex: 1 }}>
          <Group justify="space-between" mb={6}>
            <Text fw={600} size="sm">
              {area.name}
            </Text>
            <Badge color={badgeColor} variant="light">
              {label}
            </Badge>
          </Group>
          <Text size="xs" c="dimmed" mb="xs">
            {area.coordinates}
          </Text>
          <Group justify="space-between" mb={4}>
            <Text size="sm" c="dimmed">
              Инцидентов:{" "}
              <Text span fw={600}>
                {area.incidents}
              </Text>
            </Text>
            <Text size="xs" fw={600}>
              Индекс: {trendValue}%
            </Text>
          </Group>
          <Progress value={trendValue} color={badgeColor} radius="xl" />
        </div>
      </Group>
    </Paper>
  );
}
