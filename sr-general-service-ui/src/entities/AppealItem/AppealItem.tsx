import {
  Badge,
  Button,
  Divider,
  Group,
  Paper,
  Stack,
  Text,
} from '@mantine/core';
import { IconPhoto } from '@tabler/icons-react';
import { useState } from 'react';
import type { AppealData } from '../../shared/types';
import { AppealDetailsModal } from '../AppealDetailsModal/AppealDetailsModal';

interface AppealItemProps {
  appeal: AppealData;
  allAppeals?: AppealData[];
  onReply?: (id: string) => void | Promise<void>;
  onDelete?: (id: string) => void | Promise<void>;
  actionLoading?: boolean;
  showActions?: boolean;
}

export function AppealItem({
  appeal,
  allAppeals = [],
  onReply,
  onDelete,
  actionLoading = false,
  showActions = false,
}: AppealItemProps) {
  const [modalOpened, setModalOpened] = useState(false);

  const getStatusBadge = () => {
    switch (appeal.status) {
      case 'new':
        return <Badge color="blue">Новое</Badge>;
      case 'in_progress':
        return <Badge color="orange">В работе</Badge>;
      case 'completed':
        return <Badge color="green">Решено</Badge>;
      case 'declined':
        return <Badge color="red">Отклонено</Badge>;
      default:
        return null;
    }
  };

  const getPriorityBadge = () => {
    switch (appeal.priority) {
      case 'high':
        return <Badge color="red">Высокий</Badge>;
      case 'medium':
        return <Badge color="orange">Средний</Badge>;
      case 'low':
        return <Badge color="green">Низкий</Badge>;
      default:
        return null;
    }
  };

  const getActionButtons = () => {
    if (!showActions || appeal.status === 'completed' || appeal.status === 'declined') {
      return null;
    }

    if (appeal.status === 'new') {
      return (
        <Group gap="sm">
          <Button
            variant="light"
            color="blue"
            size="sm"
            loading={actionLoading}
            onClick={() => void onReply?.(appeal.id)}
          >
            Ответить
          </Button>
          <Button
            variant="light"
            color="red"
            size="sm"
            loading={actionLoading}
            onClick={() => void onDelete?.(appeal.id)}
          >
            Отклонить
          </Button>
        </Group>
      );
    }

    if (appeal.status === 'in_progress') {
      return (
        <Group gap="sm">
          <Button
            variant="light"
            color="blue"
            size="sm"
            loading={actionLoading}
            onClick={() => void onReply?.(appeal.id)}
          >
            Ответить
          </Button>
        </Group>
      );
    }

    return null;
  };

  const userReports = allAppeals.filter((item) => item.author === appeal.author);

  return (
    <>
      <Paper
        p="md"
        radius="md"
        withBorder
        style={{ cursor: 'pointer' }}
        onClick={() => setModalOpened(true)}
      >
        <Stack gap="md">
          <Group justify="space-between" align="flex-start" wrap="wrap" gap="md">
            <div style={{ flex: 1, minWidth: '300px' }}>
              <Group gap="sm" mb="xs" wrap="wrap">
                {getStatusBadge()}
                {getPriorityBadge()}
                {appeal.isDangerousArea ? <Badge color="dark">Опасная зона</Badge> : null}
              </Group>
              <Text fw={600} size="md" mb="xs">
                {appeal.title}
              </Text>
              <Text size="sm" c="dimmed" mb="sm">
                {appeal.description}
              </Text>
              <Group gap="md" mt="xs" wrap="wrap">
                <Text size="xs" c="dimmed">
                  {appeal.author}
                </Text>
                <Text size="xs" c="dimmed">
                  {appeal.date}
                </Text>
                {appeal.photosCount > 0 ? (
                  <Group gap={4}>
                    <IconPhoto size={14} />
                    <Text size="xs" c="dimmed">
                      {appeal.photosCount} фото
                    </Text>
                  </Group>
                ) : null}
              </Group>
            </div>
            {getActionButtons() ? (
              <div style={{ alignSelf: 'flex-start' }} onClick={(event) => event.stopPropagation()}>
                {getActionButtons()}
              </div>
            ) : null}
          </Group>

          {appeal.status === 'completed' && appeal.answer ? (
            <>
              <Divider />
              <div>
                <Text fw={600} size="sm" mb="xs">
                  Ответ:
                </Text>
                <Text size="sm" c="dimmed">
                  {appeal.answer}
                </Text>
              </div>
            </>
          ) : null}
        </Stack>
      </Paper>
      <AppealDetailsModal
        opened={modalOpened}
        onClose={() => setModalOpened(false)}
        appeal={appeal}
        onReply={onReply}
        onDelete={onDelete}
        actionLoading={actionLoading}
        showActions={showActions}
        userReports={userReports}
      />
    </>
  );
}
