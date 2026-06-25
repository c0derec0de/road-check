import { useMemo, useState } from "react";
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
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
} from "@mantine/core";
import { useMutation, useQuery } from "@tanstack/react-query";
import { notifications } from "@mantine/notifications";
import {
  IconCloud,
  IconEdit,
  IconMap2,
  IconPlus,
  IconSearch,
  IconTrash,
} from "@tabler/icons-react";
import {
  managerRegionsApi,
  regionsApi,
  type ManagerRegionPayload,
  type Region,
} from "../../shared/api/regionsApi";

type RegionFormState = {
  name: string;
  code: string;
  city: string;
  latitude: number | "";
  longitude: number | "";
  description: string;
};

const initialForm: RegionFormState = {
  name: "",
  code: "",
  city: "",
  latitude: "",
  longitude: "",
  description: "",
};

const toNumberOrNull = (value: unknown): number | null => {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
};

const mapRegionRecord = (item: unknown): Region | null => {
  if (!item || typeof item !== "object") {
    return null;
  }

  const source = item as Record<string, unknown>;
  const id = toNumberOrNull(source.id);
  if (id === null) {
    return null;
  }

  return {
    id,
    name: String(
      source.name ?? source.regName ?? source.regionName ?? "",
    ).trim(),
    code:
      String(source.code ?? source.regCode ?? source.regionCode ?? "").trim() ||
      null,
    city:
      String(source.city ?? source.capital ?? source.centerCity ?? "").trim() ||
      null,
    latitude:
      toNumberOrNull(source.latitude) ??
      toNumberOrNull(source.centerLat) ??
      toNumberOrNull(source.lat),
    longitude:
      toNumberOrNull(source.longitude) ??
      toNumberOrNull(source.centerLng) ??
      toNumberOrNull(source.lng),
    description: String(source.description ?? source.desc ?? "").trim() || null,
  };
};

const normalizeRegions = (payload: unknown): Region[] => {
  if (Array.isArray(payload)) {
    return payload
      .map(mapRegionRecord)
      .filter((region): region is Region => region !== null);
  }

  if (
    payload &&
    typeof payload === "object" &&
    Array.isArray((payload as { items?: unknown[] }).items)
  ) {
    return (payload as { items: unknown[] }).items
      .map(mapRegionRecord)
      .filter((region): region is Region => region !== null);
  }

  return [];
};

export function RegionsPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [editingRegion, setEditingRegion] = useState<Region | null>(null);
  const [selectedRegionId, setSelectedRegionId] = useState<number | null>(null);
  const [form, setForm] = useState<RegionFormState>(initialForm);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const regionsQuery = useQuery({
    queryKey: ["manager", "regions"],
    queryFn: async () => normalizeRegions(await managerRegionsApi.list()),
  });

  const weatherQuery = useQuery({
    queryKey: ["regions", "weather", selectedRegionId],
    queryFn: async () =>
      selectedRegionId ? regionsApi.getWeather(selectedRegionId) : null,
    enabled: selectedRegionId !== null,
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: ManagerRegionPayload) => {
      if (editingRegion) {
        return managerRegionsApi.update(editingRegion.id, payload);
      }

      return managerRegionsApi.create(payload);
    },
    onSuccess: async () => {
      notifications.show({
        title: "Готово",
        message: editingRegion ? "Регион обновлён" : "Регион создан",
        color: "green",
      });
      regionsQuery.refetch();
      setIsModalOpen(false);
      setEditingRegion(null);
      setForm(initialForm);
    },
    onError: () => {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось сохранить регион",
        color: "red",
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => managerRegionsApi.delete(id),
    onSuccess: async () => {
      notifications.show({
        title: "Готово",
        message: "Регион удалён",
        color: "green",
      });
      regionsQuery.refetch();
    },
    onError: () => {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось удалить регион",
        color: "red",
      });
    },
  });

  const regions = regionsQuery.data ?? [];

  const filteredRegions = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return regions;
    }

    return regions.filter((region) =>
      [region.id, region.name, region.code, region.city, region.description]
        .map((value) => String(value ?? "").toLowerCase())
        .join(" ")
        .includes(query),
    );
  }, [regions, searchQuery]);

  const openCreate = () => {
    setEditingRegion(null);
    setForm(initialForm);
    setIsModalOpen(true);
  };

  const openEdit = (region: Region) => {
    setEditingRegion(region);
    setForm({
      name: region.name ?? "",
      code: region.code ?? "",
      city: region.city ?? "",
      latitude: region.latitude ?? "",
      longitude: region.longitude ?? "",
      description: region.description ?? "",
    });
    setIsModalOpen(true);
  };

  const submit = async () => {
    const name = form.name.trim();
    if (!name) {
      notifications.show({
        title: "Ошибка",
        message: "Укажите название региона",
        color: "red",
      });
      return;
    }

    await saveMutation.mutateAsync({
      name,
      code: form.code.trim() || undefined,
      city: form.city.trim() || undefined,
      latitude: form.latitude === "" ? undefined : Number(form.latitude),
      longitude: form.longitude === "" ? undefined : Number(form.longitude),
      description: form.description.trim() || undefined,
    });
  };

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper p="md" className="app-hero">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Group gap="sm">
              <ThemeIcon radius="sm" size={32} color="blue" variant="light">
                <IconMap2 size={18} />
              </ThemeIcon>
              <Stack gap={2}>
                <Title order={2} className="app-hero-title">
                  Регионы
                </Title>
                <Text className="app-hero-copy">
                  Справочник регионов с погодой, базовыми координатами и
                  настройками для аналитики.
                </Text>
              </Stack>
            </Group>
            <Button
              leftSection={<IconPlus size={16} />}
              color="dark"
              onClick={openCreate}
            >
              Создать регион
            </Button>
          </Group>
        </Paper>

        <Group align="stretch" grow>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Всего регионов</Text>
            <Text className="app-value">{filteredRegions.length}</Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Погода выбранного региона</Text>
            <Text className="app-value" style={{ fontSize: 20 }}>
              {weatherQuery.data?.temperature != null
                ? `${weatherQuery.data.temperature}${weatherQuery.data.unit ?? "°"}`
                : "—"}
            </Text>
            <Text size="sm" c="dimmed">
              {weatherQuery.data?.condition ?? "Выберите регион в таблице"}
            </Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Риск по погоде</Text>
            <Badge color="orange" variant="light" mt="sm">
              {weatherQuery.data?.riskLevel ?? "Нет данных"}
            </Badge>
          </Paper>
        </Group>

        <Paper p="md" className="app-surface">
          <TextInput
            leftSection={<IconSearch size={16} />}
            placeholder="Поиск по названию, коду или городу"
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
                  <Table.Th>Название</Table.Th>
                  <Table.Th>Код</Table.Th>
                  <Table.Th>Город</Table.Th>
                  <Table.Th>Координаты</Table.Th>
                  <Table.Th>Погода</Table.Th>
                  <Table.Th>Действия</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {filteredRegions.map((region) => (
                  <Table.Tr
                    key={region.id}
                    onClick={() => setSelectedRegionId(region.id)}
                    style={{ cursor: "pointer" }}
                  >
                    <Table.Td>{region.id}</Table.Td>
                    <Table.Td>
                      <Stack gap={2}>
                        <Text fw={600}>{region.name || "-"}</Text>
                        <Text size="xs" c="dimmed">
                          {region.description || "Без описания"}
                        </Text>
                      </Stack>
                    </Table.Td>
                    <Table.Td>{region.code || "-"}</Table.Td>
                    <Table.Td>{region.city || "-"}</Table.Td>
                    <Table.Td>
                      {region.latitude != null && region.longitude != null
                        ? `${region.latitude}, ${region.longitude}`
                        : "-"}
                    </Table.Td>
                    <Table.Td>
                      {selectedRegionId === region.id && weatherQuery.data ? (
                        <Group gap={6}>
                          <IconCloud size={14} />
                          <Text size="sm">
                            {`Температура ${weatherQuery.data.temperature}C°, скорось ветра ${weatherQuery.data.windSpeedKmh}`}
                          </Text>
                        </Group>
                      ) : (
                        "Нажмите для просмотра"
                      )}
                    </Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <ActionIcon
                          variant="light"
                          color="blue"
                          onClick={(event) => {
                            event.stopPropagation();
                            openEdit(region);
                          }}
                        >
                          <IconEdit size={16} />
                        </ActionIcon>
                        <ActionIcon
                          variant="light"
                          color="red"
                          onClick={(event) => {
                            event.stopPropagation();
                            deleteMutation.mutate(region.id);
                          }}
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
          {!regionsQuery.isLoading && filteredRegions.length === 0 ? (
            <Text c="dimmed" mt="sm">
              Регионы не найдены
            </Text>
          ) : null}
        </Paper>
      </Stack>

      <Modal
        opened={isModalOpen}
        onClose={() => !saveMutation.isPending && setIsModalOpen(false)}
        title={editingRegion ? "Редактировать регион" : "Создать регион"}
        size="lg"
        centered
      >
        <Stack gap="md">
          <Group grow>
            <TextInput
              label="Название"
              value={form.name}
              onChange={(event) =>
                setForm((prev) => ({
                  ...prev,
                  name: event.currentTarget.value,
                }))
              }
              required
            />
            <TextInput
              label="Код"
              value={form.code}
              onChange={(event) =>
                setForm((prev) => ({
                  ...prev,
                  code: event.currentTarget.value,
                }))
              }
            />
          </Group>
          <TextInput
            label="Город / административный центр"
            value={form.city}
            onChange={(event) =>
              setForm((prev) => ({ ...prev, city: event.currentTarget.value }))
            }
          />
          <Group grow>
            <NumberInput
              label="Широта"
              value={form.latitude}
              onChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  latitude: typeof value === "number" ? value : "",
                }))
              }
              decimalScale={6}
            />
            <NumberInput
              label="Долгота"
              value={form.longitude}
              onChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  longitude: typeof value === "number" ? value : "",
                }))
              }
              decimalScale={6}
            />
          </Group>
          <Textarea
            label="Описание"
            minRows={3}
            value={form.description}
            onChange={(event) =>
              setForm((prev) => ({
                ...prev,
                description: event.currentTarget.value,
              }))
            }
          />
          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() => setIsModalOpen(false)}
              disabled={saveMutation.isPending}
            >
              Отмена
            </Button>
            <Button
              color="dark"
              onClick={() => void submit()}
              loading={saveMutation.isPending}
            >
              Сохранить
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
}
