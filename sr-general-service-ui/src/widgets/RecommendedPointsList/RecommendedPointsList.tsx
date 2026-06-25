import { Stack, Title } from '@mantine/core';
import { PatrolPointItem } from '../../entities/PatrolPointItem/PatrolPointItem';
import type { PatrolPointData } from '../../shared/types';

interface RecommendedPointsListProps {
  points: PatrolPointData[];
}

export function RecommendedPointsList({ points }: RecommendedPointsListProps) {
  return (
    <Stack gap="md">
      <Title order={3} fw={600} mb="sm">
        Рекомендуемые точки патрулирования
      </Title>
      <Stack gap="md">
        {points.map((point) => (
          <PatrolPointItem key={point.id} point={point} />
        ))}
      </Stack>
    </Stack>
  );
}

