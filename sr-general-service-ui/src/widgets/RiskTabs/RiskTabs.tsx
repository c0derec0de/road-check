import {
  Badge,
  Card,
  Group,
  Progress,
  SimpleGrid,
  Stack,
  Tabs,
  Text,
  Title,
} from '@mantine/core';
import { PredictionChartsSection } from '../PredictionChartsSection/PredictionChartsSection';
import type { AreaRiskData } from '../../shared/types';

interface RiskTabsProps {
  children: React.ReactNode;
  areas: AreaRiskData[];
  showTrends?: boolean;
}

const riskLabel = (level: AreaRiskData['riskLevel']) => {
  if (level === 'high') return 'Высокий';
  if (level === 'medium') return 'Средний';
  return 'Низкий';
};

const riskColor = (level: AreaRiskData['riskLevel']) => {
  if (level === 'high') return 'red';
  if (level === 'medium') return 'orange';
  return 'green';
};

export function RiskTabs({ children, areas, showTrends = true }: RiskTabsProps) {
  const maxIncidents = Math.max(1, ...areas.map((area) => area.incidents));

  return (
    <Tabs defaultValue="heatmap" radius="md" color="blue">
      <Tabs.List>
        <Tabs.Tab value="heatmap">Тепловая карта</Tabs.Tab>
        <Tabs.Tab value="analytics">Аналитика</Tabs.Tab>
        {showTrends ? <Tabs.Tab value="trends">Тренды</Tabs.Tab> : null}
      </Tabs.List>

      <Tabs.Panel value="heatmap" pt="md">
        {children}
      </Tabs.Panel>

      <Tabs.Panel value="analytics" pt="md">
        <PredictionChartsSection />
      </Tabs.Panel>

      {showTrends ? (
        <Tabs.Panel value="trends" pt="md">
          <Stack gap="md">
            <Group justify="space-between">
              <Title order={4}>Тренды по опасным участкам</Title>
              <Badge color="blue" variant="light">
                Топ-{areas.length}
              </Badge>
            </Group>

            <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
              {areas.map((area) => (
                <Card key={area.id} withBorder radius="md" p="md">
                  <Group justify="space-between" align="flex-start">
                    <Text fw={600} lineClamp={1}>
                      {area.name}
                    </Text>
                    <Badge color={riskColor(area.riskLevel)} variant="light">
                      {riskLabel(area.riskLevel)}
                    </Badge>
                  </Group>
                  <Text size="sm" c="dimmed" mt={4}>
                    Координаты: {area.coordinates}
                  </Text>
                  <Group justify="space-between" mt="sm" mb={6}>
                    <Text size="sm">Инцидентов: {area.incidents}</Text>
                    <Text size="sm" fw={600}>
                      {Math.round((area.incidents / maxIncidents) * 100)}%
                    </Text>
                  </Group>
                  <Progress
                    value={(area.incidents / maxIncidents) * 100}
                    color={riskColor(area.riskLevel)}
                    radius="xl"
                  />
                </Card>
              ))}
            </SimpleGrid>
          </Stack>
        </Tabs.Panel>
      ) : null}
    </Tabs>
  );
}
