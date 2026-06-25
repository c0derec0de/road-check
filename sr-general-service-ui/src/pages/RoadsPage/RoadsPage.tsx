import { useMemo, useState } from "react";
import {
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
import { IconPlus, IconRoad, IconSearch } from "@tabler/icons-react";
import {
  roadsApi,
  type ManagerRoad,
  type ManagerRoadPayload,
} from "../../shared/api/roadsApi";

type RoadFormState = {
  name: string;
  regionId: number | "";
  status: string;
  lengthKm: number | "";
  address: string;
  description: string;
};

const initialForm: RoadFormState = {
  name: "",
  regionId: "",
  status: "active",
  lengthKm: "",
  address: "",
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

const mapRoadRecord = (item: unknown): ManagerRoad | null => {
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
    name: String(source.name ?? source.roadName ?? source.title ?? "").trim(),
    regionId:
      toNumberOrNull(source.regionId) ??
      toNumberOrNull(source.regId) ??
      toNumberOrNull(source.region_id),
    status: String(source.status ?? source.state ?? "").trim() || null,
    lengthKm:
      toNumberOrNull(source.lengthKm) ??
      toNumberOrNull(source.length) ??
      toNumberOrNull(source.distanceKm),
    address:
      String(
        source.address ?? source.location ?? source.section ?? "",
      ).trim() || null,
    description: String(source.description ?? source.desc ?? "").trim() || null,
  };
};

const normalizeRoads = (payload: unknown): ManagerRoad[] => {
  if (Array.isArray(payload)) {
    return payload
      .map(mapRoadRecord)
      .filter((road): road is ManagerRoad => road !== null);
  }

  if (
    payload &&
    typeof payload === "object" &&
    Array.isArray((payload as { items?: unknown[] }).items)
  ) {
    return (payload as { items: unknown[] }).items
      .map(mapRoadRecord)
      .filter((road): road is ManagerRoad => road !== null);
  }

  return [];
};

export function RoadsPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [editingRoad, setEditingRoad] = useState<ManagerRoad | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState<RoadFormState>(initialForm);

  const roadsQuery = useQuery({
    queryKey: ["manager", "roads"],
    queryFn: async () => normalizeRoads(await roadsApi.list()),
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: ManagerRoadPayload) => {
      if (editingRoad) {
        return roadsApi.update(editingRoad.id, payload);
      }

      return roadsApi.create(payload);
    },
    onSuccess: async () => {
      notifications.show({
        title: "Готово",
        message: editingRoad ? "Дорога обновлена" : "Дорога создана",
        color: "green",
      });
      roadsQuery.refetch();
      setIsModalOpen(false);
      setEditingRoad(null);
      setForm(initialForm);
    },
    onError: () => {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось сохранить дорогу",
        color: "red",
      });
    },
  });

  const roads = roadsQuery.data ?? [];

  const filteredRoads = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return roads;
    }

    return roads.filter((road) =>
      [
        road.id,
        road.name,
        road.regionId,
        road.status,
        road.address,
        road.description,
      ]
        .map((value) => String(value ?? "").toLowerCase())
        .join(" ")
        .includes(query),
    );
  }, [roads, searchQuery]);

  const openCreate = () => {
    setEditingRoad(null);
    setForm(initialForm);
    setIsModalOpen(true);
  };

  const submit = async () => {
    const name = form.name.trim();
    if (!name) {
      notifications.show({
        title: "Ошибка",
        message: "Укажите название дороги",
        color: "red",
      });
      return;
    }

    await saveMutation.mutateAsync({
      name,
      regionId: form.regionId === "" ? undefined : Number(form.regionId),
      status: form.status.trim() || undefined,
      lengthKm: form.lengthKm === "" ? undefined : Number(form.lengthKm),
      address: form.address.trim() || undefined,
      description: form.description.trim() || undefined,
    });
  };

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper p="md" className="app-hero">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Group gap="sm">
              <ThemeIcon radius="sm" size={32} color="dark" variant="light">
                <IconRoad size={18} />
              </ThemeIcon>
              <Stack gap={2}>
                <Title order={2} className="app-hero-title">
                  Дороги
                </Title>
                <Text className="app-hero-copy">
                  Управление дорожной сетью, привязкой к регионам и операционным
                  статусом дорожных объектов.
                </Text>
              </Stack>
            </Group>
            <Button
              leftSection={<IconPlus size={16} />}
              color="dark"
              onClick={openCreate}
            >
              Создать дорогу
            </Button>
          </Group>
        </Paper>

        <Group grow>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Всего дорог</Text>
            <Text className="app-value">{filteredRoads.length}</Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Активные</Text>
            <Text className="app-value">
              {
                filteredRoads.filter(
                  (road) => road.status?.toLowerCase() === "active",
                ).length
              }
            </Text>
          </Paper>
          <Paper p="md" className="app-surface app-compact-card">
            <Text className="app-kicker">Общая длина, км</Text>
            <Text className="app-value">
              {filteredRoads.reduce(
                (sum, road) => sum + Number(road.lengthKm ?? 0),
                0,
              )}
            </Text>
          </Paper>
        </Group>

        <Paper p="md" className="app-surface">
          <TextInput
            leftSection={<IconSearch size={16} />}
            placeholder="Поиск по названию, региону, адресу или статусу"
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
                  <Table.Th>Регион</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {filteredRoads.map((road) => (
                  <Table.Tr key={road.id}>
                    <Table.Td>{road.id}</Table.Td>
                    <Table.Td>
                      <Stack gap={2}>
                        <Text fw={600}>{road.name || "-"}</Text>
                        <Text size="xs" c="dimmed">
                          {road.description || "Без описания"}
                        </Text>
                      </Stack>
                    </Table.Td>
                    <Table.Td>{road.regionId ?? "-"}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </ScrollArea>
          {!roadsQuery.isLoading && filteredRoads.length === 0 ? (
            <Text c="dimmed" mt="sm">
              Дороги не найдены
            </Text>
          ) : null}
        </Paper>
      </Stack>

      <Modal
        opened={isModalOpen}
        onClose={() => !saveMutation.isPending && setIsModalOpen(false)}
        title={editingRoad ? "Редактировать дорогу" : "Создать дорогу"}
        size="lg"
        centered
      >
        <Stack gap="md">
          <TextInput
            label="Название"
            value={form.name}
            onChange={(event) =>
              setForm((prev) => ({ ...prev, name: event.currentTarget.value }))
            }
            required
          />
          <Group grow>
            <NumberInput
              label="ID региона"
              value={form.regionId}
              onChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  regionId: typeof value === "number" ? value : "",
                }))
              }
              min={1}
            />
            <NumberInput
              label="Длина, км"
              value={form.lengthKm}
              onChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  lengthKm: typeof value === "number" ? value : "",
                }))
              }
              min={0}
              decimalScale={2}
            />
          </Group>
          <TextInput
            label="Статус"
            value={form.status}
            onChange={(event) =>
              setForm((prev) => ({
                ...prev,
                status: event.currentTarget.value,
              }))
            }
            placeholder="active / archived / maintenance"
          />
          <TextInput
            label="Адрес / участок"
            value={form.address}
            onChange={(event) =>
              setForm((prev) => ({
                ...prev,
                address: event.currentTarget.value,
              }))
            }
          />
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
