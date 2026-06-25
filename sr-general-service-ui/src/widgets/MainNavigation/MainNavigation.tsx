import { Box, Divider, Group, Menu, Stack, Text } from '@mantine/core';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  IconChevronDown,
  IconFileText,
  IconLogout,
  IconMap,
  IconMapPin,
  IconRoad,
  IconUsers,
  IconChartBar,
} from '@tabler/icons-react';
import { authUtils } from '../../shared/lib/auth';
import { notifications } from '@mantine/notifications';

const navIconSize = 18;

export function MainNavigation() {
  const location = useLocation();
  const navigate = useNavigate();
  const user = authUtils.getCurrentUser();
  const isGuest = authUtils.isGuest();
  const isModerator = authUtils.isModerator();

  const displayName =
    user?.name ||
    user?.login ||
    user?.email ||
    (isGuest ? 'Гость' : 'Пользователь');

  const navItems = [
    {
      path: '/analytics',
      label: isModerator ? 'Операционный обзор' : 'Аналитика',
      icon: <IconChartBar size={navIconSize} />,
      visible: true,
    },
    {
      path: '/appeals',
      label: 'Отчеты',
      icon: <IconFileText size={navIconSize} />,
      visible: true,
    },
    {
      path: '/dangerous-zones',
      label: 'Опасные зоны',
      icon: <IconMapPin size={navIconSize} />,
      visible: isModerator,
    },
    {
      path: '/regions',
      label: 'Регионы',
      icon: <IconMap size={navIconSize} />,
      visible: isModerator,
    },
    {
      path: '/roads',
      label: 'Дороги',
      icon: <IconRoad size={navIconSize} />,
      visible: isModerator,
    },
    {
      path: '/users',
      label: 'Пользователи',
      icon: <IconUsers size={navIconSize} />,
      visible: isModerator,
    },
  ];

  const handleLogout = async () => {
    try {
      const response = await authUtils.logout();
      notifications.show({
        title: 'Выход выполнен',
        message: response.message || 'Вы успешно вышли из системы',
        color: 'blue',
      });
    } catch {
      notifications.show({
        title: 'Выход выполнен',
        message: 'Сессия очищена локально',
        color: 'blue',
      });
    } finally {
      navigate('/login');
    }
  };

  return (
    <Box className="app-sidebar">
      <Stack gap={0} h="100%">
        <Box className="app-sidebar-brand">
          <Text className="app-sidebar-title">Дорожный контроль</Text>
          <Text className="app-sidebar-subtitle">Единая диспетчерская панель</Text>
        </Box>

        <Divider />

        <Stack gap={4} className="app-sidebar-nav">
          {navItems
            .filter((item) => item.visible)
            .map((item) => (
              <button
                key={item.path}
                className="app-sidebar-link"
                data-active={location.pathname === item.path}
                type="button"
                onClick={() => navigate(item.path)}
              >
                <span className="app-sidebar-link-icon">{item.icon}</span>
                <span>{item.label}</span>
              </button>
            ))}
        </Stack>

        <Box className="app-sidebar-footer">
          <Divider mb="md" />
          <Menu width={232} position="top-start">
            <Menu.Target>
              <button className="app-sidebar-user" type="button">
                <Group gap="sm" wrap="nowrap">
                  <Box className="app-sidebar-avatar">
                    {String(displayName).slice(0, 1).toUpperCase()}
                  </Box>
                  <Box style={{ minWidth: 0, flex: 1 }}>
                    <Text size="sm" fw={700} lineClamp={1}>
                      {displayName}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {isGuest ? 'Гостевой доступ' : isModerator ? 'Модератор' : 'Пользователь'}
                    </Text>
                  </Box>
                  <IconChevronDown size={15} style={{ color: '#667085' }} />
                </Group>
              </button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Label>{isGuest ? 'Гостевой режим' : 'Профиль'}</Menu.Label>
              <Menu.Item
                leftSection={<IconLogout size={16} />}
                onClick={() => void handleLogout()}
                color="red"
              >
                Выйти
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Box>
      </Stack>
    </Box>
  );
}
