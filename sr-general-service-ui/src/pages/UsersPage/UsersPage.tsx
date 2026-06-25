import { useEffect, useMemo, useState } from 'react';
import {
  ActionIcon,
  Badge,
  Button,
  Container,
  Group,
  Modal,
  Paper,
  ScrollArea,
  Stack,
  Table,
  Text,
  TextInput,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconEdit, IconSearch, IconTrash, IconUsers } from '@tabler/icons-react';
import { ApiRequestError } from '../../shared/api/apiClient';
import { usersApi, type ManagerUser, type UpdateManagerUserRequest } from '../../shared/api/usersApi';

type UserFormState = UpdateManagerUserRequest;

const getFullName = (user: ManagerUser): string => {
  return [user.lastname, user.firstname, user.middlename].filter(Boolean).join(' ').trim();
};

const getSearchableValue = (user: ManagerUser): string => {
  return [
    user.id,
    user.telegramName,
    user.firstname,
    user.middlename,
    user.lastname,
    user.department,
    user.city,
    user.phone,
    user.email,
    user.walletAddress,
  ]
    .map((value) => String(value ?? '').toLowerCase())
    .join(' ');
};

const toFormState = (user: ManagerUser): UserFormState => ({
  telegramName: user.telegramName ?? '',
  firstname: user.firstname ?? '',
  middlename: user.middlename ?? '',
  lastname: user.lastname ?? '',
  department: user.department ?? '',
  city: user.city ?? '',
  phone: user.phone ?? '',
  email: user.email ?? '',
  walletAddress: user.walletAddress ?? '',
});

const getDeleteUserErrorMessage = (error: unknown): string => {
  const responseMessage = error instanceof ApiRequestError ? error.responseMessage : undefined;

  if (
    responseMessage?.includes('reports_police_user_id_fkey') ||
    responseMessage?.includes('referenced from table "reports"')
  ) {
    return 'Пользователь связан с обращениями. Сначала переназначьте или удалите связанные обращения, затем повторите удаление пользователя.';
  }

  return 'Не удалось удалить пользователя';
};

export function UsersPage() {
  const [users, setUsers] = useState<ManagerUser[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [form, setForm] = useState<UserFormState>({
    telegramName: '',
    firstname: '',
    middlename: '',
    lastname: '',
    department: '',
    city: '',
    phone: '',
    email: '',
    walletAddress: '',
  });
  const [isSaving, setIsSaving] = useState(false);
  const [deletingUserId, setDeletingUserId] = useState<number | null>(null);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await usersApi.list();
      setUsers(data ?? []);
    } catch {
      notifications.show({
        title: 'Ошибка',
        message: 'Не удалось загрузить список пользователей',
        color: 'red',
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return users;
    }
    return users.filter((user) => getSearchableValue(user).includes(query));
  }, [users, searchQuery]);

  const startEdit = (user: ManagerUser) => {
    setEditingUserId(user.id);
    setForm(toFormState(user));
  };

  const closeEditModal = () => {
    if (isSaving) {
      return;
    }
    setEditingUserId(null);
  };

  const updateForm = (patch: Partial<UserFormState>) => {
    setForm((prev) => ({ ...prev, ...patch }));
  };

  const handleSave = async () => {
    if (editingUserId === null) {
      return;
    }

    setIsSaving(true);
    try {
      await usersApi.update(editingUserId, form);
      notifications.show({
        title: 'Готово',
        message: 'Пользователь обновлен',
        color: 'green',
      });
      setEditingUserId(null);
      await loadUsers();
    } catch {
      notifications.show({
        title: 'Ошибка',
        message: 'Не удалось обновить пользователя',
        color: 'red',
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setDeletingUserId(id);
    try {
      await usersApi.delete(id);
      notifications.show({
        title: 'Готово',
        message: 'Пользователь удален',
        color: 'green',
      });
      await loadUsers();
    } catch (error) {
      notifications.show({
        title: 'Ошибка',
        message: getDeleteUserErrorMessage(error),
        color: 'red',
      });
    } finally {
      setDeletingUserId(null);
    }
  };

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper
          p="md"
          className="app-hero"
        >
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Group gap="sm">
              <ThemeIcon radius="sm" size={32} color="blue" variant="light">
                <IconUsers size={18} />
              </ThemeIcon>
              <Stack gap={2}>
                <Title order={2} className="app-hero-title">Пользователи</Title>
                <Text className="app-hero-copy">Просмотр и управление пользователями</Text>
              </Stack>
            </Group>
            <Badge color="blue" variant="light" size="md">
              Всего: {filteredUsers.length}
            </Badge>
          </Group>
        </Paper>

        <Paper p="md" className="app-surface">
          <TextInput
            leftSection={<IconSearch size={16} />}
            placeholder="Поиск по ФИО, email, телефону, городу, отделу..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.currentTarget.value)}
          />
        </Paper>

        <Paper p="md" className="app-surface">
          <ScrollArea>
            <Table striped highlightOnHover withTableBorder withColumnBorders>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>ID</Table.Th>
                  <Table.Th>ФИО</Table.Th>
                  <Table.Th>Telegram</Table.Th>
                  <Table.Th>Отдел</Table.Th>
                  <Table.Th>Город</Table.Th>
                  <Table.Th>Телефон</Table.Th>
                  <Table.Th>Email</Table.Th>
                  <Table.Th>Wallet</Table.Th>
                  <Table.Th>Blockchain</Table.Th>
                  <Table.Th>Действия</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {filteredUsers.map((user) => (
                  <Table.Tr key={user.id}>
                    <Table.Td>{user.id}</Table.Td>
                    <Table.Td>{getFullName(user) || '-'}</Table.Td>
                    <Table.Td>{user.telegramName || '-'}</Table.Td>
                    <Table.Td>{user.department || '-'}</Table.Td>
                    <Table.Td>{user.city || '-'}</Table.Td>
                    <Table.Td>{user.phone || '-'}</Table.Td>
                    <Table.Td>{user.email || '-'}</Table.Td>
                    <Table.Td>{user.walletAddress || '-'}</Table.Td>
                    <Table.Td>
                      <Badge color={user.blockchainVerified ? 'teal' : 'gray'} variant="light">
                        {user.blockchainVerified ? 'Подтвержден' : 'Не подтвержден'}
                      </Badge>
                    </Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <ActionIcon
                          variant="light"
                          color="blue"
                          aria-label="Редактировать пользователя"
                          onClick={() => startEdit(user)}
                          disabled={isSaving || deletingUserId === user.id}
                        >
                          <IconEdit size={16} />
                        </ActionIcon>
                        <ActionIcon
                          variant="light"
                          color="red"
                          aria-label="Удалить пользователя"
                          onClick={() => void handleDelete(user.id)}
                          loading={deletingUserId === user.id}
                          disabled={isSaving}
                        >
                          <IconTrash size={16} />
                        </ActionIcon>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </ScrollArea>
          {!isLoading && filteredUsers.length === 0 ? (
            <Text c="dimmed" mt="sm">
              Пользователи не найдены
            </Text>
          ) : null}
          {isLoading ? (
            <Text c="dimmed" mt="sm">
              Загрузка пользователей...
            </Text>
          ) : null}
        </Paper>
      </Stack>

      <Modal
        opened={editingUserId !== null}
        onClose={closeEditModal}
        title="Редактирование пользователя"
        size="lg"
        centered
        closeOnClickOutside={!isSaving}
      >
        <Stack gap="sm">
          <TextInput
            label="Telegram"
            value={form.telegramName}
            onChange={(event) => updateForm({ telegramName: event.currentTarget.value })}
          />
          <Group grow>
            <TextInput
              label="Фамилия"
              value={form.lastname}
              onChange={(event) => updateForm({ lastname: event.currentTarget.value })}
            />
            <TextInput
              label="Имя"
              value={form.firstname}
              onChange={(event) => updateForm({ firstname: event.currentTarget.value })}
            />
            <TextInput
              label="Отчество"
              value={form.middlename}
              onChange={(event) => updateForm({ middlename: event.currentTarget.value })}
            />
          </Group>
          <Group grow>
            <TextInput
              label="Отдел"
              value={form.department}
              onChange={(event) => updateForm({ department: event.currentTarget.value })}
            />
            <TextInput
              label="Город"
              value={form.city}
              onChange={(event) => updateForm({ city: event.currentTarget.value })}
            />
          </Group>
          <Group grow>
            <TextInput
              label="Телефон"
              value={form.phone}
              onChange={(event) => updateForm({ phone: event.currentTarget.value })}
            />
            <TextInput
              label="Email"
              type="email"
              value={form.email}
              onChange={(event) => updateForm({ email: event.currentTarget.value })}
            />
          </Group>
          <TextInput
            label="Wallet address"
            value={form.walletAddress}
            onChange={(event) => updateForm({ walletAddress: event.currentTarget.value })}
          />

          <Group justify="flex-end" mt="sm">
            <Button variant="default" onClick={closeEditModal} disabled={isSaving}>
              Отмена
            </Button>
            <Button onClick={() => void handleSave()} loading={isSaving}>
              Сохранить
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
}
