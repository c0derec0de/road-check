import { Paper, Group, Text, Badge, Button, Stack, Box } from "@mantine/core";
import { IconMapPin, IconClock } from "@tabler/icons-react";
import type { PatrolPointData } from "../../shared/types";

interface PatrolPointItemProps {
  point: PatrolPointData;
}

export function PatrolPointItem({ point }: PatrolPointItemProps) {
  const getRiskBadgeColor = () => {
    switch (point.riskLevel) {
      case "high":
        return "red";
      case "medium":
        return "orange";
      default:
        return "green";
    }
  };

  const getRiskLabel = () => {
    switch (point.riskLevel) {
      case "high":
        return "Высокий";
      case "medium":
        return "Средний";
      default:
        return "Низкий";
    }
  };

  const getBackgroundColor = () => {
    switch (point.riskLevel) {
      case "high":
        return "#fff7f7";
      case "medium":
        return "#fffaf0";
      default:
        return "#f8fbf7";
    }
  };

  return (
    <Paper
      p="md"
      className="app-surface"
      style={{
        background: getBackgroundColor(),
        position: "relative",
      }}
    >
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" wrap="wrap" gap="md">
          <Group
            gap="md"
            align="flex-start"
            style={{ flex: 1, minWidth: "250px" }}
          >
            <Box
              style={{
                width: "34px",
                height: "34px",
                borderRadius: "8px",
                backgroundColor: "#fff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "14px",
                border: "1px solid #eef2f6",
                flexShrink: 0,
              }}
            >
              {point.number}
            </Box>
            <div style={{ flex: 1, minWidth: "200px" }}>
              <Group
                justify="space-between"
                mb="xs"
                align="flex-start"
                wrap="wrap"
                gap="sm"
              >
                <Text fw={600} size="md" style={{ flex: 1, minWidth: "200px" }}>
                  {point.address}
                </Text>
                <Group gap="xs" wrap="nowrap">
                  <Text
                    fw={700}
                    size="xl"
                    c={getRiskBadgeColor()}
                    style={{ lineHeight: 1 }}
                  >
                    {point.riskPercentage}%
                  </Text>
                  <Badge color={getRiskBadgeColor()} variant="light" size="sm">
                    {getRiskLabel()}
                  </Badge>
                </Group>
              </Group>
              <Group gap="md" mb="xs" wrap="wrap">
                <Group gap={4}>
                  <IconClock size={16} style={{ color: "#868e96" }} />
                  <Text size="sm" c="dimmed">
                    {point.time}
                  </Text>
                </Group>
                <Text size="sm" c="dimmed">
                  {point.reason}
                </Text>
              </Group>
            </div>
          </Group>
        </Group>
        <Button
          leftSection={<IconMapPin size={18} />}
          variant="light"
          color="blue"
          fullWidth
          size="sm"
        >
          Начать патрулирование
        </Button>
      </Stack>
    </Paper>
  );
}
