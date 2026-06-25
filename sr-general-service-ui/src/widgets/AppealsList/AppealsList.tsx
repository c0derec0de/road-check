import { Stack } from '@mantine/core';
import { AppealItem } from '../../entities/AppealItem/AppealItem';
import type { AppealData } from '../../shared/types';

interface AppealsListProps {
  appeals: AppealData[];
  allAppeals?: AppealData[];
  onReply?: (id: string) => void | Promise<void>;
  onDelete?: (id: string) => void | Promise<void>;
  actionLoadingId?: string | null;
  showActions?: boolean;
}

export function AppealsList({
  appeals,
  allAppeals = appeals,
  onReply,
  onDelete,
  actionLoadingId,
  showActions = false,
}: AppealsListProps) {
  return (
    <Stack gap="md">
      {appeals.map((appeal) => (
        <AppealItem
          key={appeal.id}
          appeal={appeal}
          allAppeals={allAppeals}
          onReply={onReply}
          onDelete={onDelete}
          actionLoading={actionLoadingId === appeal.id}
          showActions={showActions}
        />
      ))}
    </Stack>
  );
}
