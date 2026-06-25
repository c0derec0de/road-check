import {
  Badge,
  Box,
  Button,
  Card,
  Divider,
  Group,
  Loader,
  Modal,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from "@mantine/core";
import {
  IconChevronRight,
  IconCheck,
  IconClock,
  IconListDetails,
  IconMapPin,
  IconMessageCircle2,
  IconPhoto,
  IconShieldCheck,
  IconTrash,
  IconUser,
} from "@tabler/icons-react";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from "react-leaflet";
import {
  reportsApi,
  type ReportDetailsResponse,
} from "../../shared/api/reportsApi";
import type { AppealData } from "../../shared/types";

interface AppealDetailsModalProps {
  opened: boolean;
  onClose: () => void;
  appeal: AppealData | null;
  onReply?: (id: string) => void | Promise<void>;
  onDelete?: (id: string) => void | Promise<void>;
  actionLoading?: boolean;
  showActions?: boolean;
  userReports?: AppealData[];
}

const emptyValue = "—";

const formatDateTime = (value?: string | null): string => {
  if (!value) return emptyValue;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const statusMeta = (status?: string | null) => {
  const normalized = status?.toUpperCase();
  if (normalized === "NEW" || status === "new") {
    return { color: "blue", label: "Новое" };
  }
  if (normalized === "IN_PROGRESS" || status === "in_progress") {
    return { color: "orange", label: "В работе" };
  }
  if (normalized === "COMPLETED" || status === "completed") {
    return { color: "green", label: "Решено" };
  }
  if (normalized === "DECLINED" || status === "declined") {
    return { color: "red", label: "Отклонено" };
  }
  return { color: "gray", label: status || "Нет данных" };
};

const riskMeta = (risk?: string | null) => {
  const normalized = risk?.toLowerCase();
  if (normalized === "high") return { color: "red", label: "Высокий" };
  if (normalized === "medium") return { color: "orange", label: "Средний" };
  if (normalized === "low") return { color: "green", label: "Низкий" };
  return { color: "gray", label: risk || "Нет данных" };
};

function SectionHeader({
  icon,
  title,
  color = "blue",
}: {
  icon: ReactNode;
  title: string;
  color?: string;
}) {
  return (
    <Group gap={8} wrap="nowrap">
      <ThemeIcon variant="light" color={color} size={26}>
        {icon}
      </ThemeIcon>
      <Text fw={700} size="sm">
        {title}
      </Text>
    </Group>
  );
}

function DetailItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Box>
      <Text c="dimmed" size="xs" mb={2}>
        {label}
      </Text>
      <Text size="sm" fw={600} style={{ overflowWrap: "anywhere" }}>
        {value || emptyValue}
      </Text>
    </Box>
  );
}

function DetailGrid({
  items,
  columns = 2,
}: {
  items: Array<{ label: string; value: ReactNode }>;
  columns?: number;
}) {
  return (
    <SimpleGrid cols={{ base: 1, sm: columns }} spacing="xs" verticalSpacing={10}>
      {items.map((item) => (
        <DetailItem key={item.label} label={item.label} value={item.value} />
      ))}
    </SimpleGrid>
  );
}

function MapResizeHandler() {
  const map = useMap();

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      map.invalidateSize();
    }, 80);

    return () => window.clearTimeout(timeoutId);
  }, [map]);

  return null;
}

function ReportLocationMap({
  lat,
  lng,
  title,
}: {
  lat?: number | null;
  lng?: number | null;
  title: string;
}) {
  if (lat == null || lng == null) {
    return (
      <Paper withBorder p="sm" radius="md" style={{ background: "#f8fafc" }}>
        <Text size="sm" c="dimmed">
          Координаты не указаны, карту построить нельзя
        </Text>
      </Paper>
    );
  }

  const center: [number, number] = [lat, lng];

  return (
    <MapContainer
      key={`${lat}-${lng}`}
      center={center}
      zoom={15}
      style={{ width: "100%", height: 190, borderRadius: 6 }}
      scrollWheelZoom={false}
      dragging
    >
      <MapResizeHandler />
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <CircleMarker
        center={center}
        radius={9}
        pathOptions={{
          color: "#0f3b5c",
          fillColor: "#0f3b5c",
          fillOpacity: 0.72,
          weight: 3,
        }}
      >
        <Popup>
          <Text size="sm" fw={600}>
            {title}
          </Text>
          <Text size="xs" c="dimmed">
            {lat}, {lng}
          </Text>
        </Popup>
      </CircleMarker>
    </MapContainer>
  );
}

export function AppealDetailsModal({
  opened,
  onClose,
  appeal,
  onReply,
  onDelete,
  actionLoading = false,
  showActions = false,
  userReports = [],
}: AppealDetailsModalProps) {
  const [details, setDetails] = useState<ReportDetailsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [userReportsOpened, setUserReportsOpened] = useState(false);

  const reportId = useMemo(() => {
    if (!appeal?.id) return null;
    const parsed = Number(appeal.id);
    return Number.isFinite(parsed) ? parsed : null;
  }, [appeal?.id]);

  useEffect(() => {
    if (!opened) return;
    if (!reportId) {
      setDetails(null);
      setError("Некорректный ID отчета");
      return;
    }

    let mounted = true;
    setIsLoading(true);
    setError(null);

    void reportsApi
      .getById(reportId)
      .then((response) => {
        if (mounted) setDetails(response);
      })
      .catch(() => {
        if (mounted) {
          setDetails(null);
          setError("Не удалось загрузить подробные данные отчета");
        }
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [opened, reportId]);

  if (!appeal) return null;

  const status = statusMeta(details?.status ?? appeal.status);
  const risk = riskMeta(details?.riskLevel ?? appeal.priority);
  const title = details?.title || appeal.title;
  const description = details?.description || appeal.description;
  const address = details?.address || appeal.location || "Адрес не указан";
  const location = details?.location;
  const hasDangerousZone = details?.isDangerousZone ?? appeal.isDangerousArea;
  const photos = details?.photos ?? appeal.attachments ?? [];
  const relatedUserReports = userReports.filter((report) => report.id !== appeal.id);
  const completedWithoutAnswer =
    !appeal.answer && details?.status?.toUpperCase() === "COMPLETED";
  const canShowActions =
    showActions && appeal.status !== "completed" && appeal.status !== "declined";
  const canDecline = canShowActions && appeal.status === "new";
  const handleReply = () => {
    onClose();
    void onReply?.(appeal.id);
  };
  const handleDelete = () => {
    onClose();
    void onDelete?.(appeal.id);
  };

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      size={1040}
      centered
      title={null}
      padding="md"
    >
      <Stack gap="sm">
        <Paper p="md" className="app-hero">
          <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
            <Stack gap={8} style={{ flex: 1, minWidth: 280 }}>
              <Group gap="xs" wrap="wrap">
                <Badge color="gray" variant="light">
                  ID {appeal.id}
                </Badge>
                <Badge color={status.color} variant="light">
                  {status.label}
                </Badge>
                <Badge color={risk.color} variant="light">
                  Риск: {risk.label}
                </Badge>
                {hasDangerousZone ? (
                  <Badge color="dark" variant="light">
                    Опасная зона
                  </Badge>
                ) : null}
              </Group>

              <Title order={3} className="app-panel-title">
                {title}
              </Title>

              <Group gap={6} c="dimmed" wrap="nowrap">
                <IconMapPin size={16} />
                <Text size="sm" lineClamp={2}>
                  {address}
                </Text>
              </Group>
            </Stack>

            <SimpleGrid cols={2} spacing="xs" style={{ minWidth: 240 }}>
              <DetailItem
                label="Создано"
                value={formatDateTime(details?.createdAt ?? appeal.dateTime ?? appeal.date)}
              />
              <DetailItem label="Обновлено" value={formatDateTime(details?.updatedAt)} />
            </SimpleGrid>
          </Group>
        </Paper>

        {isLoading ? (
          <Group justify="center" py="xl">
            <Loader />
          </Group>
        ) : null}

        {!isLoading && error ? (
          <Paper withBorder p="md" radius="md">
            <Text c="red" size="sm">
              {error}
            </Text>
          </Paper>
        ) : null}

        {!isLoading ? (
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="sm" verticalSpacing="sm">
            <Stack gap="sm">
              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <SectionHeader
                    icon={<IconMessageCircle2 size={15} />}
                    title="Содержание отчета"
                  />
                  <Text size="sm" style={{ lineHeight: 1.45 }}>
                    {description}
                  </Text>
                </Stack>
              </Card>

              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <SectionHeader icon={<IconUser size={15} />} title="Заявитель" color="teal" />
                  <DetailGrid
                    items={[
                      { label: "ID", value: details?.user?.id },
                      { label: "Логин", value: details?.user?.username ?? appeal.author },
                      { label: "ФИО", value: details?.user?.fullName },
                      { label: "Телефон", value: details?.user?.phone },
                    ]}
                  />
                </Stack>
              </Card>

              {(appeal.answer || completedWithoutAnswer) ? (
                <Paper withBorder p="md" radius="md" style={{ background: "#f0fdf4" }}>
                  <Stack gap="xs">
                    <SectionHeader icon={<IconCheck size={15} />} title="Ответ по отчету" color="green" />
                    <Text size="sm" style={{ lineHeight: 1.45 }}>
                      {appeal.answer ||
                        "Отчет завершен, но текст ответа не указан в карточке."}
                    </Text>
                  </Stack>
                </Paper>
              ) : null}
            </Stack>

            <Stack gap="sm">
              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <SectionHeader icon={<IconMapPin size={15} />} title="Локация" color="blue" />
                  <ReportLocationMap
                    lat={location?.lat}
                    lng={location?.lng}
                    title={title}
                  />
                  <DetailGrid
                    items={[
                      { label: "Адрес", value: address },
                      { label: "Широта", value: location?.lat },
                      { label: "Долгота", value: location?.lng },
                    ]}
                  />
                </Stack>
              </Card>

              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <Group justify="space-between" align="center" gap="sm">
                    <SectionHeader
                      icon={<IconListDetails size={15} />}
                      title="Отчеты пользователя"
                      color="indigo"
                    />
                    <Badge color="gray" variant="light">
                      {relatedUserReports.length + 1}
                    </Badge>
                  </Group>

                  <Text size="sm" c="dimmed">
                    {details?.user?.username ?? appeal.author}
                  </Text>

                  <Button
                    variant="light"
                    size="sm"
                    rightSection={<IconChevronRight size={15} />}
                    onClick={() => setUserReportsOpened((current) => !current)}
                  >
                    {userReportsOpened ? "Скрыть происшествия" : "Посмотреть все происшествия"}
                  </Button>

                  {userReportsOpened ? (
                    <Stack gap="xs">
                      {[appeal, ...relatedUserReports].map((report) => (
                        <Paper
                          key={report.id}
                          withBorder
                          p="xs"
                          radius="sm"
                          style={{
                            background: report.id === appeal.id ? "#edf4f8" : "#ffffff",
                          }}
                        >
                          <Group justify="space-between" gap="xs" wrap="nowrap">
                            <Stack gap={2} style={{ minWidth: 0 }}>
                              <Text size="sm" fw={600} lineClamp={1}>
                                {report.title}
                              </Text>
                              <Text size="xs" c="dimmed" lineClamp={1}>
                                {report.location ?? report.date}
                              </Text>
                            </Stack>
                            <Badge
                              color={statusMeta(report.status).color}
                              variant="light"
                              style={{ flexShrink: 0 }}
                            >
                              {statusMeta(report.status).label}
                            </Badge>
                          </Group>
                        </Paper>
                      ))}
                    </Stack>
                  ) : null}
                </Stack>
              </Card>

              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <SectionHeader
                    icon={<IconShieldCheck size={15} />}
                    title="Верификация"
                    color="grape"
                  />
                  <DetailGrid
                    items={[
                      {
                        label: "Пользователь",
                        value: details?.user?.blockchainVerified ? "Да" : "Нет",
                      },
                      {
                        label: "Отчет",
                        value: details?.blockchainVerified ? "Да" : "Нет",
                      },
                      { label: "Номер блока", value: details?.blockchainBlockNumber },
                      { label: "TxHash", value: details?.blockchainTxHash },
                    ]}
                  />
                </Stack>
              </Card>

              <Card withBorder radius="md" p="md">
                <Stack gap="sm">
                  <SectionHeader icon={<IconPhoto size={15} />} title="Фотоматериалы" color="cyan" />
                  {photos.length ? (
                    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
                      {photos.map((photo, index) => (
                        <Paper key={`${photo}-${index}`} withBorder p="xs" radius="sm">
                          <Text size="xs" lineClamp={1} style={{ overflowWrap: "anywhere" }}>
                            {photo}
                          </Text>
                        </Paper>
                      ))}
                    </SimpleGrid>
                  ) : (
                    <Text size="sm" c="dimmed">
                      Фото отсутствуют
                    </Text>
                  )}
                </Stack>
              </Card>
            </Stack>
          </SimpleGrid>
        ) : null}

        {!isLoading && details?.comments?.length ? (
          <Card withBorder radius="md" p="md">
            <Stack gap="sm">
              <SectionHeader
                icon={<IconMessageCircle2 size={15} />}
                title="Комментарии"
                color="indigo"
              />
              <SimpleGrid cols={{ base: 1, md: 2 }} spacing="xs">
                {details.comments.map((comment) => (
                  <Paper
                    key={comment.id}
                    withBorder
                    p="sm"
                    radius="md"
                    style={{ background: "#f8fafc" }}
                  >
                    <Group justify="space-between" gap="xs" mb={4} wrap="nowrap">
                      <Text size="sm" fw={600} lineClamp={1}>
                        {comment.user}
                      </Text>
                      <Text size="xs" c="dimmed" style={{ whiteSpace: "nowrap" }}>
                        {formatDateTime(comment.createdAt)}
                      </Text>
                    </Group>
                    <Text size="sm" lineClamp={3} style={{ lineHeight: 1.4 }}>
                      {comment.text}
                    </Text>
                  </Paper>
                ))}
              </SimpleGrid>
            </Stack>
          </Card>
        ) : null}

        <Divider />
        <Group justify="space-between" gap="sm">
          <Group gap={6} c="dimmed">
            <IconClock size={15} />
            <Text size="xs">
              Подробности загружаются из карточки отчета и API
            </Text>
          </Group>
          <Group gap="xs" justify="flex-end">
            {canShowActions ? (
              <Button
                variant="light"
                color="blue"
                leftSection={<IconMessageCircle2 size={15} />}
                loading={actionLoading}
                onClick={handleReply}
              >
                Ответить
              </Button>
            ) : null}
            {canDecline ? (
              <Button
                variant="light"
                color="red"
                leftSection={<IconTrash size={15} />}
                loading={actionLoading}
                onClick={handleDelete}
              >
                Отклонить
              </Button>
            ) : null}
            <Button variant="light" onClick={onClose}>
              Закрыть
            </Button>
          </Group>
        </Group>
      </Stack>
    </Modal>
  );
}
