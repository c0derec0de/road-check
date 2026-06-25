import { Badge, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { DangerousAreaItem } from '../../entities/DangerousAreaItem/DangerousAreaItem';
import type { AreaRiskData } from '../../shared/types';

interface DangerousAreasListProps {
  areas: AreaRiskData[];
}

export function DangerousAreasList({ areas }: DangerousAreasListProps) {
  return (
    <Paper withBorder radius="sm" p="lg" className="app-surface">
      <Group justify="space-between" mb="md">
        <div>
          <Title order={3} fw={700}>
            Самые опасные участки
          </Title>
          <Text size="sm" c="dimmed" mt={4}>
            Топ-5 участков с наибольшим количеством инцидентов
          </Text>
        </div>
        <Badge color="red" variant="light">
          Топ-{areas.length}
        </Badge>
      </Group>

      <Stack gap="sm">
        {areas.map((area) => (
          <DangerousAreaItem key={area.id} area={area} />
        ))}
      </Stack>
    </Paper>
  );
}
