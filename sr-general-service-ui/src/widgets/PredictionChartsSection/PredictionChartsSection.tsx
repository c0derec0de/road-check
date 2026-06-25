import { Card, Grid, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core';
import { useEffect, useState } from 'react';
import { predictionsApi } from '../../shared/api/predictionsApi';

interface ChartCardState {
  title: string;
  src: string | null;
  error: string | null;
}

export function PredictionChartsSection() {
  const [isLoading, setIsLoading] = useState(true);
  const [charts, setCharts] = useState<ChartCardState[]>([
    { title: 'Риск по зонам', src: null, error: null },
    { title: 'Динамика по месяцам', src: null, error: null },
    { title: 'Причины инцидентов', src: null, error: null },
  ]);

  useEffect(() => {
    let isMounted = true;
    const urlsToRevoke: string[] = [];

    const loadCharts = async () => {
      setIsLoading(true);
      const requests: Array<Promise<Blob>> = [predictionsApi.getRiskChart(), predictionsApi.getMonthlyChart(), predictionsApi.getCausesChart()];
      const results = await Promise.allSettled(requests);

      if (!isMounted) return;

      const nextCharts: ChartCardState[] = results.map((result, index) => {
        const title = charts[index]?.title ?? `График ${index + 1}`;
        if (result.status === 'fulfilled') {
          const objectUrl = URL.createObjectURL(result.value);
          urlsToRevoke.push(objectUrl);
          return { title, src: objectUrl, error: null };
        }
        return { title, src: null, error: 'Не удалось загрузить график с сервера' };
      });

      setCharts(nextCharts);
      setIsLoading(false);
    };

    void loadCharts();

    return () => {
      isMounted = false;
      urlsToRevoke.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);

  const renderCard = (chart: ChartCardState, large = false) => (
    <Card key={chart.title} withBorder radius="md" p="lg" style={{ minHeight: large ? 560 : 460 }}>
      <Stack gap="sm" h="100%">
        <Text fw={600}>{chart.title}</Text>
        {chart.src ? (
          <img
            src={chart.src}
            alt={chart.title}
            style={{
              width: '100%',
              height: '100%',
              minHeight: large ? 460 : 360,
              objectFit: 'contain',
              borderRadius: 8,
              background: '#fff',
            }}
          />
        ) : (
          <Paper withBorder radius="md" p="md" style={{ minHeight: large ? 460 : 360, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Text c="dimmed" ta="center" size="sm">
              {isLoading ? 'Загрузка графика...' : chart.error ?? 'Нет данных'}
            </Text>
          </Paper>
        )}
      </Stack>
    </Card>
  );

  return (
    <Paper withBorder p="lg" radius="md">
      <Stack gap="md">
        <Group justify="space-between" align="center">
          <Title order={3}>Прогнозные графики</Title>
          {isLoading && <Loader size="sm" />}
        </Group>

        <Grid gutter="lg">
          <Grid.Col span={{ base: 12, lg: 6 }}>{charts[0] && renderCard(charts[0])}</Grid.Col>
          <Grid.Col span={{ base: 12, lg: 6 }}>{charts[1] && renderCard(charts[1])}</Grid.Col>
          <Grid.Col span={12}>{charts[2] && renderCard(charts[2], true)}</Grid.Col>
        </Grid>
      </Stack>
    </Paper>
  );
}

