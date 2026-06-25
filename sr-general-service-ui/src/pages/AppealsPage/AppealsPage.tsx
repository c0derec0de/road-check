import {
  Badge,
  Box,
  Button,
  Card,
  Container,
  Divider,
  Group,
  Modal,
  NumberInput,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
} from "@mantine/core";
import { useEffect, useMemo, useState } from "react";
import { notifications } from "@mantine/notifications";
import {
  IconFileDescription,
  IconMapPin,
  IconNote,
  IconShieldCheck,
} from "@tabler/icons-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppealsSummary } from "../../widgets/AppealsSummary/AppealsSummary";
import { FilterAndSearch } from "../../features/FilterAndSearch/FilterAndSearch";
import { AppealsList } from "../../widgets/AppealsList/AppealsList";
import {
  reportsApi,
  type CreateReportRequest,
} from "../../shared/api/reportsApi";
import { authUtils } from "../../shared/lib/auth";
import type {
  AppealData,
  AppealPriority,
  AppealStatus,
  AppealSummaryData,
} from "../../shared/types";

type RawReport = {
  id?: number | string | null;
  title?: string | null;
  incidentType?: string | null;
  description?: string | null;
  comment?: string | null;
  username?: string | null;
  userId?: number | string | null;
  date?: string | null;
  createdAt?: string | null;
  status?: string | null;
  riskLevel?: string | null;
  isDangerousZone?: boolean | null;
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  photosUuid?: string | null;
  photos?: string[] | null;
};

type CreateFormState = {
  policeUserId: number | "";
  userId: number | "";
  incidentType: string;
  description: string;
  latitude: number | "";
  longitude: number | "";
  fatalities: number | "";
  injuries: number | "";
  cause: string;
};

const DEFAULT_USER_POLICE_ID = 1;

const formatNumber = (value: number): string =>
  new Intl.NumberFormat("ru-RU").format(value);

const fallbackSummary = (appeals: AppealData[]): AppealSummaryData[] => {
  const total = appeals.length;
  const newCount = appeals.filter((appeal) => appeal.status === "new").length;
  const inProgressCount = appeals.filter(
    (appeal) => appeal.status === "in_progress",
  ).length;
  const completedCount = appeals.filter(
    (appeal) => appeal.status === "completed",
  ).length;

  return [
    {
      title: "Всего отчётов",
      value: formatNumber(total),
      change: "0%",
      changeType: "neutral",
      iconType: "alert",
    },
    {
      title: "Новые",
      value: formatNumber(newCount),
      change: "0%",
      changeType: "neutral",
      iconType: "alert",
    },
    {
      title: "В работе",
      value: formatNumber(inProgressCount),
      change: "0%",
      changeType: "neutral",
      iconType: "trending",
    },
    {
      title: "Решено",
      value: formatNumber(completedCount),
      change: "0%",
      changeType: "neutral",
      iconType: "trending",
    },
  ];
};

const mapStatus = (status?: string | null): AppealStatus => {
  const normalized = status?.toUpperCase();
  if (normalized === "IN_PROGRESS") return "in_progress";
  if (normalized === "CONFIRMED" || normalized === "COMPLETED")
    return "completed";
  if (normalized === "DECLINED") return "declined";
  return "new";
};

const mapPriority = (riskLevel?: string | null): AppealPriority => {
  const normalized = riskLevel?.toLowerCase();
  if (normalized === "high") return "high";
  if (normalized === "low") return "low";
  return "medium";
};

const formatDate = (date?: string | null): string => {
  if (!date) return "Дата неизвестна";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return date;
  return parsed.toLocaleDateString("ru-RU");
};

const formatDateTime = (date?: string | null): string => {
  if (!date) return "Дата неизвестна";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return date;
  return parsed.toLocaleString("ru-RU");
};

const splitPhotos = (report: RawReport): string[] => {
  if (Array.isArray(report.photos)) return report.photos.filter(Boolean);
  if (!report.photosUuid) return [];
  return report.photosUuid
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
};

const mapReportToAppeal = (report: RawReport): AppealData => {
  const sourceDate = report.date ?? report.createdAt;
  const photos = splitPhotos(report);
  const location =
    report.address ||
    (report.latitude != null && report.longitude != null
      ? `${report.latitude}, ${report.longitude}`
      : undefined);

  return {
    id: String(report.id ?? ""),
    title: report.title || report.incidentType || "Без заголовка",
    description: report.description || "Описание отсутствует",
    author:
      report.username ||
      (report.userId ? `Пользователь #${report.userId}` : "Пользователь"),
    date: formatDate(sourceDate),
    status: mapStatus(report.status),
    priority: mapPriority(report.riskLevel),
    isDangerousArea: report.isDangerousZone ?? false,
    photosCount: photos.length,
    answer: report.comment || undefined,
    type: "other",
    location,
    dateTime: formatDateTime(sourceDate),
    attachments: photos,
  };
};

const toOptionalNumber = (value: number | ""): number | undefined => {
  if (value === "") return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const initialCreateForm: CreateFormState = {
  policeUserId: "",
  userId: "",
  incidentType: "",
  description: "",
  latitude: "",
  longitude: "",
  fatalities: "",
  injuries: "",
  cause: "",
};

export function AppealsPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<AppealStatus | null>(null);
  const [priorityFilter, setPriorityFilter] = useState<AppealPriority | null>(
    null,
  );
  const [createModalOpened, setCreateModalOpened] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);
  const [replyAppeal, setReplyAppeal] = useState<AppealData | null>(null);
  const [replyComment, setReplyComment] = useState("");
  const [createForm, setCreateForm] = useState<CreateFormState>({
    ...initialCreateForm,
  });
  const queryClient = useQueryClient();
  const isModerator = authUtils.isModerator();
  const currentUserId = authUtils.getCurrentUser()?.userId;

  const updateCreateForm = (patch: Partial<CreateFormState>) => {
    setCreateForm((prev) => ({ ...prev, ...patch }));
  };

  const pageSize = 200;

  const appealsQuery = useQuery({
    queryKey: ["reports", "list", pageSize],
    queryFn: async (): Promise<AppealData[]> => {
      const firstPage = await reportsApi.list({ page: 1, size: pageSize });
      return (firstPage.reports ?? [])
        .map(mapReportToAppeal)
        .filter((appeal) => appeal.id !== "");
    },
  });

  const appeals = appealsQuery.data ?? [];
  const summary = useMemo(() => fallbackSummary(appeals), [appeals]);

  useEffect(() => {
    if (!appealsQuery.error) {
      return;
    }

    notifications.show({
      title: "Ошибка",
      message: "Не удалось загрузить отчёты из API",
      color: "red",
    });
  }, [appealsQuery.error]);

  const refreshAppealsData = async () => {
    await queryClient.invalidateQueries({ queryKey: ["reports"] });
    await queryClient.invalidateQueries({ queryKey: ["traffic-dashboard"] });
  };

  const createReportMutation = useMutation({
    mutationFn: (payload: CreateReportRequest) => reportsApi.create(payload),
  });

  const confirmReportMutation = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment: string }) =>
      reportsApi.confirm(id, { comment }),
  });

  const declineReportMutation = useMutation({
    mutationFn: (id: number) => reportsApi.decline(id),
  });

  const resetCreateForm = () => {
    setCreateForm({
      ...initialCreateForm,
      policeUserId: isModerator ? "" : DEFAULT_USER_POLICE_ID,
      userId: isModerator ? "" : (currentUserId ?? ""),
    });
  };

  useEffect(() => {
    if (!createModalOpened) {
      return;
    }

    setCreateForm((prev) => ({
      ...prev,
      policeUserId: isModerator
        ? prev.policeUserId
        : prev.policeUserId || DEFAULT_USER_POLICE_ID,
      userId: isModerator ? prev.userId : (currentUserId ?? ""),
    }));
  }, [createModalOpened, currentUserId, isModerator]);

  const handleCreateReport = async () => {
    try {
      const incidentType = String(createForm.incidentType ?? "").trim();
      const userId = isModerator ? createForm.userId : currentUserId;
      const policeUserId = isModerator
        ? createForm.policeUserId
        : DEFAULT_USER_POLICE_ID;

      if (!incidentType || !userId || !policeUserId) {
        notifications.show({
          title: "Ошибка",
          message: isModerator
            ? "Заполните обязательные поля: ID сотрудника, ID заявителя и тип инцидента"
            : "Не удалось определить текущего пользователя или не заполнен тип инцидента",
          color: "red",
        });
        return;
      }

      const payload: CreateReportRequest = {
        policeUserId: Number(policeUserId),
        incidentType,
        description: String(createForm.description ?? "").trim() || undefined,
        latitude: toOptionalNumber(createForm.latitude),
        longitude: toOptionalNumber(createForm.longitude),
        fatalities: toOptionalNumber(createForm.fatalities),
        injuries: toOptionalNumber(createForm.injuries),
        cause: String(createForm.cause ?? "").trim() || undefined,
        ...(isModerator ? { userId: Number(userId) } : {}),
      };

      setIsCreating(true);
      await createReportMutation.mutateAsync(payload);
      notifications.show({
        title: "Готово",
        message: "Отчёт успешно создан",
        color: "green",
      });
      setCreateModalOpened(false);
      resetCreateForm();
      await refreshAppealsData();
    } catch {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось создать отчёт",
        color: "red",
      });
    } finally {
      setIsCreating(false);
    }
  };

  const parseReportId = (reportId: string): number | null => {
    const numericId = Number(reportId);
    if (!Number.isFinite(numericId)) {
      notifications.show({
        title: "Ошибка",
        message: "Некорректный ID отчёта",
        color: "red",
      });
      return null;
    }

    return numericId;
  };

  const handleDeclineReport = async (reportId: string) => {
    const numericId = parseReportId(reportId);
    if (numericId === null) return;

    setActionLoadingId(reportId);
    try {
      await declineReportMutation.mutateAsync(numericId);
      notifications.show({
        title: "Готово",
        message: "Отчёт отклонён",
        color: "green",
      });
      await refreshAppealsData();
    } catch {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось отклонить отчёт",
        color: "red",
      });
    } finally {
      setActionLoadingId(null);
    }
  };

  const openReplyModal = (reportId: string) => {
    const appeal = appeals.find((item) => item.id === reportId);
    if (!appeal) {
      notifications.show({
        title: "Ошибка",
        message: "Отчёт не найден в текущем списке",
        color: "red",
      });
      return;
    }

    setReplyAppeal(appeal);
    setReplyComment(appeal.answer ?? "");
  };

  const closeReplyModal = () => {
    if (confirmReportMutation.isPending) return;

    setReplyAppeal(null);
    setReplyComment("");
  };

  const handleSubmitReply = async () => {
    if (!replyAppeal) return;

    const numericId = parseReportId(replyAppeal.id);
    if (numericId === null) return;

    const comment = replyComment.trim();
    if (!comment) {
      notifications.show({
        title: "Ошибка",
        message: "Введите ответ по обращению",
        color: "red",
      });
      return;
    }

    setActionLoadingId(replyAppeal.id);
    try {
      await confirmReportMutation.mutateAsync({ id: numericId, comment });
      notifications.show({
        title: "Готово",
        message: "Ответ прикреплён, отчёт решён",
        color: "green",
      });
      setReplyAppeal(null);
      setReplyComment("");
      await refreshAppealsData();
    } catch {
      notifications.show({
        title: "Ошибка",
        message: "Не удалось отправить ответ",
        color: "red",
      });
    } finally {
      setActionLoadingId(null);
    }
  };

  const filteredAppeals = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return appeals.filter((appeal) => {
      if (statusFilter && appeal.status !== statusFilter) return false;
      if (priorityFilter && appeal.priority !== priorityFilter) return false;
      if (!query) return true;

      return [
        appeal.id,
        appeal.title,
        appeal.description,
        appeal.author,
        appeal.location ?? "",
        appeal.date,
      ].some((field) =>
        String(field ?? "")
          .toLowerCase()
          .includes(query),
      );
    });
  }, [appeals, searchQuery, statusFilter, priorityFilter]);

  return (
    <Container size="xl" className="app-page">
      <Stack gap="md">
        <Paper p="md" className="app-hero">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Box maw={760}>
              <Group gap="sm" mb={8}>
                <ThemeIcon radius="sm" size={32} color="blue" variant="light">
                  <IconFileDescription size={18} />
                </ThemeIcon>
                <Title order={2} className="app-hero-title">
                  Отчёты
                </Title>
              </Group>
            </Box>
            <Button color="dark" onClick={() => setCreateModalOpened(true)}>
              Создать отчёт
            </Button>
          </Group>
        </Paper>

        <AppealsSummary summary={summary} />

        <Paper p="md" className="app-surface">
          <FilterAndSearch
            searchQuery={searchQuery}
            statusFilter={statusFilter}
            priorityFilter={priorityFilter}
            onSearchQueryChange={setSearchQuery}
            onStatusFilterChange={setStatusFilter}
            onPriorityFilterChange={setPriorityFilter}
          />
        </Paper>

        <Paper p="md" className="app-surface">
          <Group justify="space-between" mb="sm">
            <Title order={4}>Список отчётов</Title>
            <Badge color="blue" variant="light">
              Всего: {filteredAppeals.length}
            </Badge>
          </Group>
          <AppealsList
            appeals={filteredAppeals}
            allAppeals={appeals}
            actionLoadingId={actionLoadingId}
            showActions={isModerator}
            onReply={openReplyModal}
            onDelete={(id) => void handleDeclineReport(id)}
          />
        </Paper>
      </Stack>

      <Modal
        opened={createModalOpened}
        onClose={() => !isCreating && setCreateModalOpened(false)}
        title="Новый отчёт"
        size={760}
        centered
        closeOnClickOutside={!isCreating}
      >
        <Stack gap="lg">
          <Card withBorder radius="lg" p="lg" style={{ background: "#f8fafc" }}>
            <Group gap="sm" mb="xs">
              <ThemeIcon size={34} radius="md" color="blue" variant="light">
                <IconNote size={18} />
              </ThemeIcon>
              <Box>
                <Text fw={700}>Форма создания отчёта</Text>
                <Text size="sm" c="dimmed">
                  Заполните основную информацию об инциденте. Обязательным
                  остаётся только тип инцидента.
                </Text>
              </Box>
            </Group>
            {!isModerator ? (
              <Text size="sm" c="dimmed">
                Заявитель будет определён автоматически из вашего JWT.
              </Text>
            ) : null}
          </Card>

          <Stack gap="md">
            <Box>
              <Group gap="xs" mb="xs">
                <IconShieldCheck size={16} />
                <Text fw={600}>Основные сведения</Text>
              </Group>
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                {isModerator ? (
                  <NumberInput
                    label="ID сотрудника полиции"
                    value={createForm.policeUserId}
                    onChange={(value) =>
                      updateCreateForm({
                        policeUserId: typeof value === "number" ? value : "",
                      })
                    }
                    min={1}
                    required
                    placeholder="Например, 15"
                  />
                ) : null}
                {isModerator ? (
                  <NumberInput
                    label="ID заявителя"
                    value={createForm.userId}
                    onChange={(value) =>
                      updateCreateForm({
                        userId: typeof value === "number" ? value : "",
                      })
                    }
                    min={1}
                    required
                    placeholder="Например, 103"
                  />
                ) : (
                  <TextInput
                    label="Заявитель"
                    value={
                      currentUserId
                        ? `Текущий пользователь #${currentUserId}`
                        : "Определяется по JWT"
                    }
                    disabled
                  />
                )}
                <TextInput
                  label="Тип инцидента"
                  placeholder="Например, ДТП, яма, неработающий светофор"
                  value={createForm.incidentType}
                  onChange={(event) =>
                    updateCreateForm({
                      incidentType: event.currentTarget.value ?? "",
                    })
                  }
                  required
                />
                <TextInput
                  label="Причина"
                  placeholder="Например, гололёд, плохая видимость"
                  value={createForm.cause}
                  onChange={(event) =>
                    updateCreateForm({ cause: event.currentTarget.value ?? "" })
                  }
                />
              </SimpleGrid>
            </Box>

            <Divider />

            <Box>
              <Group gap="xs" mb="xs">
                <IconMapPin size={16} />
                <Text fw={600}>Место происшествия</Text>
              </Group>
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                <NumberInput
                  label="Широта"
                  value={createForm.latitude}
                  onChange={(value) =>
                    updateCreateForm({
                      latitude: typeof value === "number" ? value : "",
                    })
                  }
                  min={-90}
                  max={90}
                  decimalScale={6}
                  placeholder="55.7558"
                />
                <NumberInput
                  label="Долгота"
                  value={createForm.longitude}
                  onChange={(value) =>
                    updateCreateForm({
                      longitude: typeof value === "number" ? value : "",
                    })
                  }
                  min={-180}
                  max={180}
                  decimalScale={6}
                  placeholder="37.6176"
                />
              </SimpleGrid>
            </Box>

            <Divider />

            <Box>
              <Text fw={600} mb="xs">
                Описание и последствия
              </Text>
              <Textarea
                label="Описание"
                minRows={4}
                placeholder="Опишите, что произошло, когда это было замечено и что важно учесть при проверке."
                value={createForm.description}
                onChange={(event) =>
                  updateCreateForm({
                    description: event.currentTarget.value ?? "",
                  })
                }
              />
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" mt="md">
                <NumberInput
                  label="Погибшие"
                  value={createForm.fatalities}
                  onChange={(value) =>
                    updateCreateForm({
                      fatalities: typeof value === "number" ? value : "",
                    })
                  }
                  min={0}
                  placeholder="0"
                />
                <NumberInput
                  label="Пострадавшие"
                  value={createForm.injuries}
                  onChange={(value) =>
                    updateCreateForm({
                      injuries: typeof value === "number" ? value : "",
                    })
                  }
                  min={0}
                  placeholder="0"
                />
              </SimpleGrid>
            </Box>
          </Stack>

          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() => setCreateModalOpened(false)}
              disabled={isCreating}
            >
              Отмена
            </Button>
            <Button
              color="dark"
              onClick={() => void handleCreateReport()}
              loading={isCreating}
            >
              Создать отчёт
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={Boolean(replyAppeal)}
        onClose={closeReplyModal}
        title="Ответ по обращению"
        size={640}
        centered
        closeOnClickOutside={!confirmReportMutation.isPending}
      >
        <Stack gap="md">
          {replyAppeal ? (
            <Card withBorder radius="md" p="md" style={{ background: "#f8fafc" }}>
              <Text fw={700}>{replyAppeal.title}</Text>
              <Text size="sm" c="dimmed" lineClamp={3}>
                {replyAppeal.description}
              </Text>
            </Card>
          ) : null}

          <Textarea
            label="Сообщение"
            placeholder="Напишите ответ, который будет прикреплён к обращению"
            minRows={5}
            value={replyComment}
            onChange={(event) => setReplyComment(event.currentTarget.value)}
            required
          />

          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={closeReplyModal}
              disabled={confirmReportMutation.isPending}
            >
              Отмена
            </Button>
            <Button
              color="dark"
              onClick={() => void handleSubmitReply()}
              loading={confirmReportMutation.isPending}
            >
              Отправить ответ
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
}
