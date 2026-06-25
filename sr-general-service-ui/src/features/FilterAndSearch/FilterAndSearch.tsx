import { Group, Select, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import type { AppealPriority, AppealStatus } from '../../shared/types';

interface FilterAndSearchProps {
  searchQuery: string;
  statusFilter: AppealStatus | null;
  priorityFilter: AppealPriority | null;
  onSearchQueryChange: (value: string) => void;
  onStatusFilterChange: (value: AppealStatus | null) => void;
  onPriorityFilterChange: (value: AppealPriority | null) => void;
}

export function FilterAndSearch({
  searchQuery,
  statusFilter,
  priorityFilter,
  onSearchQueryChange,
  onStatusFilterChange,
  onPriorityFilterChange,
}: FilterAndSearchProps) {
  return (
    <Group gap="sm" wrap="wrap">
        <TextInput
          placeholder="Поиск"
          leftSection={<IconSearch size={16} />}
          style={{ flex: 1, minWidth: '200px' }}
          value={searchQuery}
          onChange={(event) => onSearchQueryChange(event.currentTarget.value)}
        />
        <Select
          placeholder="Все статусы"
          data={[
            { value: 'new', label: 'Новые' },
            { value: 'in_progress', label: 'В работе' },
            { value: 'completed', label: 'Решённые' },
            { value: 'declined', label: 'Отклонённые' },
          ]}
          style={{ minWidth: '180px' }}
          value={statusFilter}
          onChange={(value) => onStatusFilterChange((value as AppealStatus | null) ?? null)}
          clearable
        />
        <Select
          placeholder="Все приоритеты"
          data={[
            { value: 'high', label: 'Высокий' },
            { value: 'medium', label: 'Средний' },
            { value: 'low', label: 'Низкий' },
          ]}
          style={{ minWidth: '180px' }}
          value={priorityFilter}
          onChange={(value) => onPriorityFilterChange((value as AppealPriority | null) ?? null)}
          clearable
        />
    </Group>
  );
}
