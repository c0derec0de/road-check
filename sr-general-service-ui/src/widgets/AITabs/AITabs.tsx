import { Tabs } from '@mantine/core';
import { AIRiskRecommendation } from '../AIRiskRecommendation/AIRiskRecommendation';
import { RecommendedPointsList } from '../RecommendedPointsList/RecommendedPointsList';
import type { AIRiskRecommendationData, PatrolPointData } from '../../shared/types';

interface AITabsProps {
  recommendation: AIRiskRecommendationData;
  patrolPoints: PatrolPointData[];
}

export function AITabs({ recommendation, patrolPoints }: AITabsProps) {
  return (
    <Tabs defaultValue="patrol-points">
      <Tabs.List>
        <Tabs.Tab value="patrol-points">Точки патрулирования</Tabs.Tab>
        <Tabs.Tab value="fixation">Фиксация</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="patrol-points" pt="xl">
        <AIRiskRecommendation data={recommendation} />
        <RecommendedPointsList points={patrolPoints} />
      </Tabs.Panel>

      <Tabs.Panel value="fixation" pt="xl">
        <div>Контент для фиксации будет здесь</div>
      </Tabs.Panel>
    </Tabs>
  );
}

