import { Card, Group, Text, Title, ThemeIcon } from '@mantine/core';
import { IconAlertTriangle, IconMapPin, IconTrendingUp, IconUsers } from '@tabler/icons-react';
import type { StatData, AppealSummaryData } from '../../types';

interface StatCardProps {
  data: StatData | AppealSummaryData;
}

const formatChange = (change: string): string => {
  const raw = change.trim();
  const match = raw.match(/^([+-]?)(\d+(?:\.\d+)?)(%)$/);
  if (!match) return raw;

  const [, sign, numeric, percentSign] = match;
  const parsed = Number(numeric);
  if (!Number.isFinite(parsed)) return raw;

  const formatted = parsed >= 10 ? parsed.toFixed(0) : parsed.toFixed(1);
  return `${sign}${formatted}${percentSign}`;
};

export function StatCard({ data }: StatCardProps) {
  const getChangeColor = () => {
    switch (data.changeType) {
      case 'positive':
        return 'green';
      case 'negative':
        return 'red';
      default:
        return 'gray';
    }
  };

  const getIcon = () => {
    switch (data.iconType) {
      case 'alert':
        return <IconAlertTriangle size={18} />;
      case 'users':
        return <IconUsers size={18} />;
      case 'trending':
        return <IconTrendingUp size={18} />;
      case 'mapPin':
        return <IconMapPin size={18} />;
      default:
        return null;
    }
  };

  const getIconColor = () => {
    switch (data.changeType) {
      case 'positive':
        return 'blue';
      case 'negative':
        return 'red';
      default:
        return 'gray';
    }
  };

  return (
    <Card padding="md" className="app-surface app-compact-card" h="100%">
      <Group justify="space-between" mb={8} align="flex-start">
        <Text size="sm" c="dimmed" fw={500} style={{ flex: 1, lineHeight: 1.35 }}>
          {data.title}
        </Text>
        {data.iconType ? (
          <ThemeIcon variant="light" size="md" radius="sm" color={getIconColor()}>
            {getIcon()}
          </ThemeIcon>
        ) : null}
      </Group>
      <Title order={2} fw={650} mb={6} className="app-value">
        {data.value}
      </Title>
      <Text size="xs" c={getChangeColor()} fw={500}>
        {formatChange(data.change)}
      </Text>
      {data.hint ? (
        <Text size="xs" c="dimmed" mt={2}>
          {data.hint}
        </Text>
      ) : null}
    </Card>
  );
}
