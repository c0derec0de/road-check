import {
  Card,
  Stack,
  Title,
  Text,
  TextInput,
  PasswordInput,
  Button,
  Group,
  SegmentedControl,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { useNavigate } from "react-router-dom";
import { authUtils } from "../../shared/lib/auth";
import { useEffect, useState } from "react";

type AuthMode = "login" | "register";

export function AuthForm() {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [mode, setMode] = useState<AuthMode>("login");

  const form = useForm({
    initialValues: {
      login: "",
      email: "",
      password: "",
      firstname: "",
      lastname: "",
      phone: "",
      walletAddress: "",
    },
    validate: {
      login: (value) => {
        if (mode === "register") {
          if (!value) {
            return "Логин обязателен";
          }
          if (value.length < 3) {
            return "Логин должен содержать не менее 3 символов";
          }
          return null;
        }
        if (!value) {
          return "Логин обязателен";
        }
        if (value.length < 3) {
          return "Логин должен содержать не менее 3 символов";
        }
        return null;
      },
      email: (value) => {
        if (mode !== "register") {
          return null;
        }
        if (!value) {
          return "Email обязателен";
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          return "Введите корректный email";
        }
        return null;
      },
      password: (value) => {
        if (!value) {
          return "Пароль обязателен";
        }
        if (value.length < 6) {
          return "Пароль должен содержать не менее 6 символов";
        }
        return null;
      },
    },
  });

  useEffect(() => {
    form.clearErrors();
  }, [mode]);

  const handleSubmit = async (values: typeof form.values) => {
    setIsSubmitting(true);
    try {
      const response =
        mode === "login"
          ? await authUtils.login({
              login: values.login,
              password: values.password,
            })
          : await authUtils.register({
              login: values.login,
              email: values.email,
              password: values.password,
              firstname: values.firstname.trim() || undefined,
              lastname: values.lastname.trim() || undefined,
              phone: values.phone.trim() || undefined,
              walletAddress: values.walletAddress.trim() || undefined,
            });

      if (response.success && response.token) {
        notifications.show({
          title: "Успешно",
          message:
            response.message ||
            (mode === "login"
              ? "Вы успешно авторизованы"
              : "Регистрация прошла успешно"),
          color: "green",
        });
        navigate("/analytics");
        return;
      }

      notifications.show({
        title: "Ошибка",
        message:
          response.message ||
          (mode === "login"
            ? "Неверный логин или пароль"
            : "Не удалось зарегистрироваться"),
        color: "red",
      });
    } catch {
      notifications.show({
        title: "Ошибка",
        message:
          mode === "login"
            ? "Не удалось выполнить вход. Проверьте подключение к серверу."
            : "Не удалось зарегистрироваться. Проверьте подключение к серверу.",
        color: "red",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card
      padding="lg"
      className="app-surface"
      style={{
        width: "100%",
        maxWidth: "420px",
        backgroundColor: "#fff",
        margin: "0 auto",
      }}
    >
      <Stack gap="md">
        <div>
          <Title
            order={2}
            fw={650}
            mb={4}
            ta="center"
            c="dark"
            style={{ fontSize: 20 }}
          >
            {mode === "login" ? "Авторизация" : "Регистрация"}
          </Title>
          <Text size="sm" c="dimmed" ta="center">
            {mode === "login" ? "" : "Создайте учетную запись"}
          </Text>
        </div>

        <SegmentedControl
          value={mode}
          onChange={(value) => setMode(value as AuthMode)}
          data={[
            { label: "Вход", value: "login" },
            { label: "Регистрация", value: "register" },
          ]}
          fullWidth
        />

        <form onSubmit={form.onSubmit(handleSubmit)}>
          <Stack gap="sm">
            <TextInput
              label="Логин"
              placeholder={
                mode === "login" ? "Введите логин" : "Придумайте логин"
              }
              required
              size="sm"
              {...form.getInputProps("login")}
            />

            {mode === "register" && (
              <>
                <TextInput
                  label="Email"
                  placeholder="Введите email"
                  required
                  size="sm"
                  {...form.getInputProps("email")}
                />
                <Group grow>
                  <TextInput
                    label="Имя"
                    placeholder="Имя"
                    size="sm"
                    {...form.getInputProps("firstname")}
                  />
                  <TextInput
                    label="Фамилия"
                    placeholder="Фамилия"
                    size="sm"
                    {...form.getInputProps("lastname")}
                  />
                </Group>
                <TextInput
                  label="Телефон"
                  placeholder="+7..."
                  size="sm"
                  {...form.getInputProps("phone")}
                />
                <TextInput
                  label="Wallet Address"
                  placeholder="0x..."
                  size="sm"
                  {...form.getInputProps("walletAddress")}
                />
              </>
            )}

            <PasswordInput
              label="Пароль"
              placeholder="Введите пароль"
              required
              size="sm"
              {...form.getInputProps("password")}
            />

            <Button
              type="submit"
              variant="filled"
              color="dark"
              loading={isSubmitting}
              fullWidth
              size="sm"
              mt="md"
              style={{
                backgroundColor: "#0f3b5c",
                fontWeight: 600,
              }}
            >
              {mode === "login" ? "Войти" : "Зарегистрироваться"}
            </Button>
          </Stack>
        </form>
      </Stack>
    </Card>
  );
}
