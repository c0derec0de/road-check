import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MantineProvider, createTheme } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import 'leaflet/dist/leaflet.css'
import './index.css'
import App from './App.tsx'

Object.assign(globalThis as { __APP_API_BASE_URL__?: string }, {
  __APP_API_BASE_URL__: import.meta.env.VITE_API_BASE_URL ?? '',
});

const theme = createTheme({
  primaryColor: 'blue',
  defaultRadius: 'sm',
  fontFamily:
    'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif',
  headings: {
    fontFamily:
      'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif',
    fontWeight: '700',
  },
  colors: {
    blue: [
      '#edf4f8',
      '#d9e6ee',
      '#b7cddd',
      '#8eadc4',
      '#6289a4',
      '#3f6b88',
      '#0f3b5c',
      '#0d334f',
      '#0a2a42',
      '#071f31',
    ],
  },
});
const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <MantineProvider theme={theme} defaultColorScheme="light">
        <Notifications position="top-right" />
        <App />
      </MantineProvider>
    </QueryClientProvider>
  </StrictMode>,
)
