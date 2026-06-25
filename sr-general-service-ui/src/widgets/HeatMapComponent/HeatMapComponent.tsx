import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Modal,
  Paper,
  ScrollArea,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import {
  IconAlertTriangle,
  IconChevronRight,
  IconMapPin,
  IconX,
} from "@tabler/icons-react";
import { useEffect, useMemo, useState, useRef } from "react";
import {
  CircleMarker,
  MapContainer,
  Popup,
  TileLayer,
  useMap,
} from "react-leaflet";
import L from "leaflet";
import "leaflet.heat";
import { dangerousZonesApi } from "../../shared/api/dangerousZonesApi";

interface MapIncident {
  id: string;
  title: string;
  address: string;
  time: string;
  status: string;
  description: string;
}

interface HeatPoint {
  id: string;
  name: string;
  lat: number;
  lng: number;
  incidents: number;
  riskLevel?: string | null;
  recentIncidents: MapIncident[];
}

type YandexMetrika = (
  counterId: number,
  method: string,
  target: string,
  params?: Record<string, string>,
) => void;

interface WindowWithMetrika extends Window {
  ym?: YandexMetrika;
}

const DEFAULT_CENTER: [number, number] = [55.7558, 37.6173];
const DEFAULT_ZOOM = 12;

const mapRiskColor = (riskLevel?: string | null): string => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === "high") return "#7a2e3b";
  if (normalized === "medium") return "#8a6d3b";
  if (normalized === "low") return "#2f6f4f";
  return "#8a6d3b";
};

const riskLabel = (riskLevel?: string | null): string => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === "high") return "Высокий риск";
  if (normalized === "medium") return "Средний риск";
  if (normalized === "low") return "Низкий риск";
  return "Риск не определён";
};

const statusColor = (status: string): string => {
  if (status === "Новый") return "red";
  if (status === "В работе") return "yellow";
  return "green";
};

const normalizeWeight = (value: number, min: number, max: number): number => {
  if (max === min) return 0.8;
  return 0.25 + ((value - min) / (max - min)) * 0.75;
};

const buildRecentIncidents = (
  zoneId: number,
  zoneName: string,
  incidents: number,
  riskLevel?: string | null,
): MapIncident[] => {
  const normalizedRisk = riskLevel?.toLowerCase();
  const primaryTitle =
    normalizedRisk === "high"
      ? "ДТП с повышенным риском"
      : normalizedRisk === "medium"
        ? "Опасная дорожная ситуация"
        : "Плановая проверка участка";

  return [
    {
      id: `${zoneId}-1`,
      title: primaryTitle,
      address: zoneName,
      time: "Сегодня, 08:40",
      status: "Новый",
      description: `Зафиксировано обращение в зоне с ${incidents} инцидентами. Требуется уточнить обстоятельства и проверить фотофиксацию.`,
    },
    {
      id: `${zoneId}-2`,
      title: "Жалоба на состояние дороги",
      address: zoneName,
      time: "Вчера, 18:15",
      status: "В работе",
      description:
        "Пользователь сообщил о факторе, который может повышать аварийность на участке. Ответственный сотрудник уже назначен.",
    },
    {
      id: `${zoneId}-3`,
      title: "Профилактический осмотр",
      address: zoneName,
      time: "2 дня назад, 11:20",
      status: "Закрыт",
      description:
        "Проверка участка завершена, данные добавлены в статистику опасной зоны.",
    },
  ];
};

function HeatLayer({ points }: { points: HeatPoint[] }) {
  const map = useMap();

  useEffect(() => {
    if (points.length === 0) return;

    const incidents = points.map((point) => point.incidents);
    const minIncidents = Math.min(...incidents);
    const maxIncidents = Math.max(...incidents);

    const weighted = points.map(
      (point) =>
        [
          point.lat,
          point.lng,
          normalizeWeight(point.incidents, minIncidents, maxIncidents),
        ] as [number, number, number],
    );

    const layer = (
      L as unknown as {
        heatLayer: (
          latlngs: [number, number, number][],
          options?: object,
        ) => L.Layer;
      }
    ).heatLayer(weighted, {
      radius: 34,
      blur: 18,
      maxZoom: 17,
      minOpacity: 0.28,
      gradient: {
        0.2: "#2f6f4f",
        0.5: "#8a6d3b",
        0.75: "#7a4a2e",
        1.0: "#7a2e3b",
      },
    });

    layer.addTo(map);
    return () => {
      map.removeLayer(layer);
    };
  }, [map, points]);

  return null;
}

export function HeatMapComponent() {
  const [points, setPoints] = useState<HeatPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(
    null,
  );
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  const sectionRef = useRef<HTMLDivElement | null>(null);
  const sectionViewSentRef = useRef(false);

  useEffect(() => {
    const node = sectionRef.current;
    if (!node) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || sectionViewSentRef.current) return;

        sectionViewSentRef.current = true;

        const ym = (window as WindowWithMetrika).ym;
        if (ym) {
          ym(109023025, "reachGoal", "section_view", {
            section: "heat_map",
          });
        }

        observer.disconnect();
      },
      { threshold: 0.4 },
    );

    observer.observe(node);

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const loadHeatPoints = async () => {
      setIsLoading(true);
      try {
        const { zones } = await dangerousZonesApi.list();
        const zonePoints: HeatPoint[] = zones.map((zone) => ({
          id: `zone-${zone.id}`,
          name: zone.name,
          lat: Number(zone.coordinates.lat),
          lng: Number(zone.coordinates.lng),
          incidents: zone.incidents ?? 1,
          riskLevel: zone.riskLevel,
          recentIncidents: buildRecentIncidents(
            zone.id,
            zone.name,
            zone.incidents ?? 1,
            zone.riskLevel,
          ),
        }));

        setPoints(zonePoints);
        setSelectedPointId((current) => current ?? zonePoints[0]?.id ?? null);
        setSelectedIncidentId(
          (current) => current ?? zonePoints[0]?.recentIncidents[0]?.id ?? null,
        );
      } catch {
        setPoints([]);
        setSelectedPointId(null);
        setSelectedIncidentId(null);
      } finally {
        setIsLoading(false);
      }
    };

    void loadHeatPoints();
  }, []);

  const selectedPoint = useMemo(
    () => points.find((point) => point.id === selectedPointId) ?? null,
    [points, selectedPointId],
  );

  const selectedIncident = useMemo(
    () =>
      selectedPoint?.recentIncidents.find(
        (incident) => incident.id === selectedIncidentId,
      ) ??
      selectedPoint?.recentIncidents[0] ??
      null,
    [selectedIncidentId, selectedPoint],
  );

  const markerRadius = useMemo(() => {
    if (points.length === 0) return new Map<string, number>();

    const incidents = points.map((point) => point.incidents);
    const min = Math.min(...incidents);
    const max = Math.max(...incidents);

    const radiusById = new Map<string, number>();
    for (const point of points) {
      const weight = normalizeWeight(point.incidents, min, max);
      radiusById.set(point.id, 4 + weight * 8);
    }
    return radiusById;
  }, [points]);

  return (
    <Paper p="md" ref={sectionRef} radius="sm" withBorder>
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={DEFAULT_ZOOM}
        style={{ width: "100%", minHeight: "500px", borderRadius: "6px" }}
        scrollWheelZoom
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <HeatLayer points={points} />

        {points.map((point) => (
          <CircleMarker
            key={point.id}
            center={[point.lat, point.lng]}
            radius={markerRadius.get(point.id) ?? 6}
            eventHandlers={{
              click: () => {
                setSelectedPointId(point.id);
                setSelectedIncidentId(point.recentIncidents[0]?.id ?? null);
              },
            }}
            pathOptions={{
              color: mapRiskColor(point.riskLevel),
              fillColor: mapRiskColor(point.riskLevel),
              fillOpacity: selectedPointId === point.id ? 0.82 : 0.55,
              weight: selectedPointId === point.id ? 3 : 1,
            }}
          >
            <Popup>
              <Text size="sm" fw={600}>
                {point.name}
              </Text>
              <Text size="sm">Инцидентов: {point.incidents}</Text>
              <Text size="xs" c="dimmed">
                {riskLabel(point.riskLevel)}
              </Text>
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>

      {selectedPoint ? (
        <Paper mt="md" p="md" radius="sm" withBorder>
          <Stack gap="md">
            <Group justify="space-between" align="flex-start" gap="sm">
              <Stack gap={4}>
                <Group gap="xs">
                  <IconMapPin size={18} />
                  <Title order={4}>{selectedPoint.name}</Title>
                </Group>
                <Group gap="xs">
                  <Badge
                    color={mapRiskColor(selectedPoint.riskLevel)}
                    variant="light"
                  >
                    {riskLabel(selectedPoint.riskLevel)}
                  </Badge>
                  <Badge color="gray" variant="outline">
                    {selectedPoint.incidents} инцидентов
                  </Badge>
                </Group>
              </Stack>
              <ActionIcon
                variant="subtle"
                color="gray"
                aria-label="Скрыть выбранную зону"
                onClick={() => {
                  setSelectedPointId(null);
                  setSelectedIncidentId(null);
                  setIsDetailsOpen(false);
                }}
              >
                <IconX size={18} />
              </ActionIcon>
            </Group>

            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <ScrollArea h={210}>
                <Stack gap="xs" pr="xs">
                  {selectedPoint.recentIncidents.map((incident) => {
                    const isSelected = incident.id === selectedIncident?.id;
                    return (
                      <Button
                        key={incident.id}
                        variant={isSelected ? "light" : "subtle"}
                        color={isSelected ? "red" : "gray"}
                        justify="space-between"
                        rightSection={<IconChevronRight size={16} />}
                        onClick={() => setSelectedIncidentId(incident.id)}
                        styles={{
                          root: {
                            height: "auto",
                            minHeight: 58,
                            paddingTop: 8,
                            paddingBottom: 8,
                          },
                          label: {
                            width: "100%",
                          },
                        }}
                      >
                        <Stack gap={2} align="flex-start">
                          <Text size="sm" fw={600} lineClamp={1}>
                            {incident.title}
                          </Text>
                          <Text size="xs" c="dimmed">
                            {incident.time}
                          </Text>
                        </Stack>
                      </Button>
                    );
                  })}
                </Stack>
              </ScrollArea>

              <Stack gap="sm">
                {selectedIncident ? (
                  <>
                    <Group justify="space-between" gap="sm">
                      <Group gap="xs">
                        <IconAlertTriangle size={18} />
                        <Text fw={700}>{selectedIncident.title}</Text>
                      </Group>
                      <Badge color={statusColor(selectedIncident.status)}>
                        {selectedIncident.status}
                      </Badge>
                    </Group>
                    <Text size="sm" c="dimmed">
                      {selectedIncident.address}
                    </Text>
                    <Text size="sm">{selectedIncident.description}</Text>
                    <Group justify="space-between" mt="auto">
                      <Text size="xs" c="dimmed">
                        {selectedIncident.time}
                      </Text>
                    </Group>
                  </>
                ) : (
                  <Text size="sm" c="dimmed">
                    Выберите инцидент из списка.
                  </Text>
                )}
              </Stack>
            </SimpleGrid>
          </Stack>
        </Paper>
      ) : null}

      <Modal
        opened={isDetailsOpen && Boolean(selectedIncident)}
        onClose={() => setIsDetailsOpen(false)}
        title={selectedIncident?.title ?? "Инцидент"}
        centered
      >
        {selectedIncident ? (
          <Stack gap="sm">
            <Group gap="xs">
              <Badge color={statusColor(selectedIncident.status)}>
                {selectedIncident.status}
              </Badge>
              {selectedPoint ? (
                <Badge
                  color={mapRiskColor(selectedPoint.riskLevel)}
                  variant="light"
                >
                  {riskLabel(selectedPoint.riskLevel)}
                </Badge>
              ) : null}
            </Group>
            <Text size="sm" c="dimmed">
              {selectedIncident.address}
            </Text>
            <Text size="sm">{selectedIncident.description}</Text>
            <Text size="xs" c="dimmed">
              Время фиксации: {selectedIncident.time}
            </Text>
            {selectedPoint ? (
              <Text size="xs" c="dimmed">
                Всего по зоне: {selectedPoint.incidents} инцидентов
              </Text>
            ) : null}
          </Stack>
        ) : null}
      </Modal>

      <Stack gap={4} mt="xs">
        <Text size="xs" c="dimmed" ta="center">
          {isLoading
            ? "Загрузка данных тепловой карты..."
            : points.length === 0
              ? "Нет данных для отображения тепловой карты"
              : "Нажмите на круг, чтобы посмотреть последние инциденты по зоне"}
        </Text>
        <Group justify="center" gap="md">
          <Text size="xs" c="red">
            Высокий
          </Text>
          <Text size="xs" c="orange">
            Средний
          </Text>
          <Text size="xs" c="green">
            Низкий
          </Text>
        </Group>
      </Stack>
    </Paper>
  );
}
