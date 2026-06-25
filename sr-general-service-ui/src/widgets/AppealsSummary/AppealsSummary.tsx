import { Grid } from '@mantine/core';
import { StatCard } from '../../shared/ui/StatCard/StatCard';
import type { AppealSummaryData } from '../../shared/types';

interface AppealsSummaryProps {
  summary: AppealSummaryData[];
}

export function AppealsSummary({ summary }: AppealsSummaryProps) {
  return (
    <Grid mb="md">
      {summary.map((stat, index) => (
        <Grid.Col key={index} span={{ base: 12, sm: 6, md: 3 }}>
          <StatCard data={stat} />
        </Grid.Col>
      ))}
    </Grid>
  );
}

