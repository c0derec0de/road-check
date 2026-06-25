import { Grid } from '@mantine/core';
import { StatCard } from '../../shared/ui/StatCard/StatCard';
import type { StatData } from '../../shared/types';

interface SummaryStatsProps {
  stats: StatData[];
}

export function SummaryStats({ stats }: SummaryStatsProps) {
  return (
    <Grid mb="md">
      {stats.map((stat, index) => (
        <Grid.Col key={index} span={{ base: 12, sm: 6, md: 3 }}>
          <StatCard data={stat} />
        </Grid.Col>
      ))}
    </Grid>
  );
}

