import { Container, Title, Box } from '@mantine/core';
import { AuthForm } from '../../features/AuthForm/AuthForm';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authUtils } from '../../shared/lib/auth';

export function LoginPage() {
  const navigate = useNavigate();

  useEffect(() => {
    if (authUtils.hasAccess()) {
      navigate('/analytics', { replace: true });
    }
  }, [navigate]);

  return (
    <Box
      style={{
        minHeight: '100vh',
        background: '#f8f9fa',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
      }}
    >
      <Container size="sm" style={{ width: '100%', maxWidth: '460px' }}>
        <Title order={1} fw={650} mb="md" ta="center" c="dark" style={{ fontSize: 24, lineHeight: 1.25 }}>
          Система мониторинга безопасного дорожного движения
        </Title>
        <AuthForm />
      </Container>
    </Box>
  );
}
