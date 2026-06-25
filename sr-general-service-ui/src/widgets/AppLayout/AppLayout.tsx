import { AppShell } from '@mantine/core';
import { MainNavigation } from '../MainNavigation/MainNavigation';
import { Outlet } from 'react-router-dom';

export function AppLayout() {
  return (
    <AppShell navbar={{ width: 280, breakpoint: 'xs' }} padding={0}>
      <AppShell.Navbar>
        <MainNavigation />
      </AppShell.Navbar>
      <AppShell.Main className="app-shell-main">
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}

