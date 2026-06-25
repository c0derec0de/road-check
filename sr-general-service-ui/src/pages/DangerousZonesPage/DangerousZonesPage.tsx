import { useMemo, useState } from 'react';
import {
  ActionIcon,
  Badge,
  Button,
  Container,
  Group,
  Modal,
  NumberInput,
  Paper,
  ScrollArea,
  SegmentedControl,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { useMutation, useQuery } from '@tanstack/react-query';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle, IconEdit, IconPlus, IconSearch, IconTrash } from '@tabler/icons-react';
import {
  managerDangerousZonesApi,
  type ManagerDangerousZone,
  type ManagerDangerousZonePayload,
} from '../../shared/api/managerDangerousZonesApi';

type ZoneFormState = {
  name: string;
  regionId: number | '';
  incidents: number | '';
  riskLevel: string;
  description: string;
  latitude: number | '';
  longitude: number | '';
};

const initialForm: ZoneFormState = {
  name: '',
  regionId: '',
  incidents: '',
  riskLevel: 'medium',
  description: '',
  latitude: '',
  longitude: '',
};

const mapRiskColor = (riskLevel?: string | null) => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === 'high') return 'red';
  if (normalized === 'low') return 'green';
  return 'orange';
};

const toNumberOrNull = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
};

const mapZoneRecord = (item: unknown): ManagerDangerousZone | null => {
  if (!item || typeof item !== 'object') {
    return null;
  }

  const source = item as Record<string, unknown>;
  const id = toNumberOrNull(source.id);
  if (id === null) {
    return null;
  }

  return {
    id,
    name: String(source.name ?? source.zoneName ?? source.title ?? '').trim(),
    regionId:
      toNumberOrNull(source.regionId) ??
      toNumberOrNull(source.regId) ??
      toNumberOrNull(source.region_id),
    incidents:
      toNumberOrNull(source.incidents) ??
      toNumberOrNull(source.incidentsCount) ??
      toNumberOrNull(source.reportCount),
    riskLevel: String(source.riskLevel ?? source.risk ?? source.level ?? '').trim() || null,
    description: String(source.description ?? source.desc ?? '').trim() || null,
    latitude:
      toNumberOrNull(source.latitude) ??
      toNumberOrNull(source.centerLat) ??
      toNumberOrNull(source.lat),
    longitude:
      toNumberOrNull(source.longitude) ??
      toNumberOrNull(source.centerLng) ??
      toNumberOrNull(source.lng),
    active:
      typeof source.active === 'boolean'
        ? source.active
        : typeof source.isActive === 'boolean'
          ? source.isActive
          : null,
  };
};

const getNormalizedZones = (payload: unknown): ManagerDangerousZone[] => {
  if (Array.isArray(payload)) {
    return payload.map(mapZoneRecord).filter((zone): zone is ManagerDangerousZone => zone !== null);
  }

  if (payload && typeof payload === 'object' && Array.isArray((payload as { items?: unknown[] }).items)) {
    return (payload as { items: unknown[] }).items
      .map(mapZoneRecord)
      .filter((zone): zone is ManagerDangerousZone => zone !== null);
  }

  return [];
};

export function DangerousZonesPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [editingZone, setEditingZone] = useState<ManagerDangerousZone | null>(null);
  const [form, setForm] = useState<ZoneFormState>(initialForm);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const zonesQuery = useQuery({
    queryKey: ['manager', 'dangerous-zones'],
    queryFn: async () => getNormalizedZones(await managerDangerousZonesApi.list()),
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: ManagerDangerousZonePayload) => {
      if (editingZone) {
        return managerDangerousZonesApi.update(editingZone.id, payload);
      }

      return managerDangerousZonesApi.create(payload);
    },
    onSuccess: async () => {
      notifications.show({
        title: 'Готово',
        message: editingZone ? 'Опасная зона обновлена' : 'Опасная зона создана',
        color: 'green',
      });
      zonesQuery.refetch();
      setIsModalOpen(false);
      setEditingZone(null);
      setForm(initialForm);
    },
    onError: () => {
      notifications.show({
        title: 'Ошибка',
        message: 'Не удалось сохранить опасную зону',
        color: 'red',
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => managerDangerousZonesApi.delete(id),
    onSuccess: async () => {
      notifications.show({
        title: 'Готово',
        message: 'Опасная зона удалена',
        color: 'green',
      });
      zonesQuery.refetch();
    },
    onError: () => {
      notifications.show({
        title: 'Ошибка',
        message: 'Не удалось удалить опасную зону',
        color: 'red',
      });
    },
  });

  const zones = zonesQuery.data ?? [];

  const filteredZones = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return zones;
    }

    return zones.filter((zone) =>
      [zone.id, zone.name, zone.regionId, zone.riskLevel, zone.description]
        .map((value) => String(value ?? '').toLowerCase())
        .join(' ')
        .includes(query),
    );
  }, [searchQuery, zones]);

  const openCreate = () => {
    setEditingZone(null);
    setForm(initialForm);
    setIsModalOpen(true);
  };

  const openEdit = (zone: ManagerDangerousZone) => {
    setEditingZone(zone);
    setForm({
      name: zone.name ?? '',
      regionId: zone.regionId ?? '',
      incidents: zone.incidents ?? '',
      riskLevel: zone.riskLevel ?? 'medium',
      description: zone.description ?? '',
      latitude: zone.latitude ?? '',
      longitude: zone.longitude ?? '',
    });
    setIsModalOpen(true);
  };

  const submit = async () => {
    const name = form.name.trim();
    if (!name) {
      notifications.show({
        title: 'Ошибка',
        message: 'Укажите название опасной зоны',
        color: 'red',
      });
      return;
    }

    await saveMutation.mutateAsync({
      name,
      regionId: form.regionId === '' ? undefined : Number(form.regionId),
      incidents: form.incidents === '' ? undefined : Number(form.incidents),
      riskLevel: form.riskLevel || undefined,
      description: form.description.trim() || undefined,
      latitude: form.latitude === '' ? undefined : Number(form.latitude),
      longitude: form.longitude === '' ? undefined : Number(form.longitude),
    });
  };

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper p="md" className="app-hero">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Group gap="sm">
              <ThemeIcon radius="sm" size={32} color="red" variant="light">
                <IconAlertTriangle size={18} />
              </ThemeIcon>
              <Stack gap={2}>
                <Title order={2} className="app-hero-title">Опасные зоны</Title>
                <Text className="app-hero-copy">
                  Реестр активных зон риска с управлением параметрами, координатами и уровнем опасности.
                </Text>
              </Stack>
            </Group>
            <Button leftSection={<IconPlus size={16} />} color="dark" onClick={openCreate}>
              Создать зону
            </Button>
          </Group>
        </Paper>

        <Group grow>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Всего зон</Text>
            <Text className="app-value">{filteredZones.length}</Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Высокий риск</Text>
            <Text className="app-value">
              {filteredZones.filter((zone) => zone.riskLevel?.toLowerCase() === 'high').length}
            </Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Инцидентов</Text>
            <Text className="app-value">
              {filteredZones.reduce((sum, zone) => sum + Number(zone.incidents ?? 0), 0)}
            </Text>
          </Paper>
        </Group>

        <Paper p="md" className="app-surface">
          <TextInput
            leftSection={<IconSearch size={16} />}
            placeholder="Поиск по названию, региону, риску или описанию"
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
                  <Table.Th>Зона</Table.Th>
                  <Table.Th>Регион</Table.Th>
                  <Table.Th>Риск</Table.Th>
                  <Table.Th>Инциденты</Table.Th>
                  <Table.Th>Координаты</Table.Th>
                  <Table.Th>Действия</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {filteredZones.map((zone) => (
                  <Table.Tr key={zone.id}>
                    <Table.Td>{zone.id}</Table.Td>
                    <Table.Td>
                      <Stack gap={2}>
                        <Text fw={600}>{zone.name || '-'}</Text>
                        <Text size="xs" c="dimmed">{zone.description || 'Без описания'}</Text>
                      </Stack>
                    </Table.Td>
                    <Table.Td>{zone.regionId ?? '-'}</Table.Td>
                    <Table.Td>
                      <Badge color={mapRiskColor(zone.riskLevel)} variant="light">
                        {zone.riskLevel ?? 'medium'}
                      </Badge>
                    </Table.Td>
                    <Table.Td>{zone.incidents ?? 0}</Table.Td>
                    <Table.Td>
                      {zone.latitude != null && zone.longitude != null
                        ? `${zone.latitude}, ${zone.longitude}`
                        : '-'}
                    </Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <ActionIcon variant="light" color="blue" onClick={() => openEdit(zone)}>
                          <IconEdit size={16} />
                        </ActionIcon>
                        <ActionIcon
                          variant="light"
                          color="red"
                          loading={deleteMutation.isPending}
                          onClick={() => deleteMutation.mutate(zone.id)}
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
          {!zonesQuery.isLoading && filteredZones.length === 0 ? (
            <Text c="dimmed" mt="sm">Опасные зоны не найдены</Text>
          ) : null}
        </Paper>
      </Stack>

      <Modal
        opened={isModalOpen}
        onClose={() => !saveMutation.isPending && setIsModalOpen(false)}
        title={editingZone ? 'Редактировать опасную зону' : 'Создать опасную зону'}
        size="lg"
        centered
      >
        <Stack gap="md">
          <TextInput
            label="Название"
            value={form.name}
            onChange={(event) => setForm((prev) => ({ ...prev, name: event.currentTarget.value }))}
            required
          />
          <Group grow>
            <NumberInput
              label="ID региона"
              value={form.regionId}
              onChange={(value) => setForm((prev) => ({ ...prev, regionId: typeof value === 'number' ? value : '' }))}
              min={1}
            />
            <NumberInput
              label="Инциденты"
              value={form.incidents}
              onChange={(value) => setForm((prev) => ({ ...prev, incidents: typeof value === 'number' ? value : '' }))}
              min={0}
            />
          </Group>
          <Stack gap={6}>
            <Text size="sm" fw={500}>Уровень риска</Text>
            <SegmentedControl
              value={form.riskLevel}
              onChange={(value) => setForm((prev) => ({ ...prev, riskLevel: value }))}
              data={[
                { label: 'High', value: 'high' },
                { label: 'Medium', value: 'medium' },
                { label: 'Low', value: 'low' },
              ]}
              fullWidth
            />
          </Stack>
          <Group grow>
            <NumberInput
              label="Широта"
              value={form.latitude}
              onChange={(value) => setForm((prev) => ({ ...prev, latitude: typeof value === 'number' ? value : '' }))}
              decimalScale={6}
            />
            <NumberInput
              label="Долгота"
              value={form.longitude}
              onChange={(value) => setForm((prev) => ({ ...prev, longitude: typeof value === 'number' ? value : '' }))}
              decimalScale={6}
            />
          </Group>
          <Textarea
            label="Описание"
            minRows={3}
            value={form.description}
            onChange={(event) => setForm((prev) => ({ ...prev, description: event.currentTarget.value }))}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setIsModalOpen(false)} disabled={saveMutation.isPending}>
              Отмена
            </Button>
            <Button color="dark" onClick={() => void submit()} loading={saveMutation.isPending}>
              Сохранить
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
}
