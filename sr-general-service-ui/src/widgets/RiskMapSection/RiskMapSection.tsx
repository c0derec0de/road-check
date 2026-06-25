import { Grid, Paper } from '@mantine/core';
import { RiskTabs } from '../RiskTabs/RiskTabs';
import { HeatMapComponent } from '../HeatMapComponent/HeatMapComponent';
import { RiskSidebar } from '../RiskSidebar/RiskSidebar';
import type { CurrentWeatherResponse } from '../../shared/api/dashboardApi';
import type { AreaRiskData } from '../../shared/types';

interface RiskMapSectionProps {
  current: CurrentWeatherResponse | null;
  areas: AreaRiskData[];
  showTrends?: boolean;
}

export function RiskMapSection({ current, areas, showTrends = true }: RiskMapSectionProps) {
  return (
    <Paper withBorder radius="sm" p="md" className="app-surface">
      <RiskTabs areas={areas} showTrends={showTrends}>
        <Grid gutter="md">
          <Grid.Col span={{ base: 12, md: 8 }}>
            <HeatMapComponent />
          </Grid.Col>
          <Grid.Col span={{ base: 12, md: 4 }}>
            <RiskSidebar current={current} />
          </Grid.Col>
        </Grid>
      </RiskTabs>
    </Paper>
  );
}
