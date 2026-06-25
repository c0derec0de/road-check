import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { authUtils } from '../../shared/lib/auth';

jest.mock('../../shared/lib/auth', () => ({
  authUtils: {
    hasAccess: jest.fn(),
    hasRole: jest.fn(),
  },
}));

const mockedHasAccess = authUtils.hasAccess as jest.Mock;
const mockedHasRole = authUtils.hasRole as jest.Mock;

describe('ProtectedRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders children when user has access', () => {
    mockedHasAccess.mockReturnValue(true);
    mockedHasRole.mockReturnValue(true);

    render(
      <MemoryRouter initialEntries={['/analytics']}>
        <Routes>
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <div>Private content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Private content')).toBeInTheDocument();
  });

  it('redirects to login when user has no access', () => {
    mockedHasAccess.mockReturnValue(false);

    render(
      <MemoryRouter initialEntries={['/analytics']}>
        <Routes>
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <div>Private content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('redirects when role is not allowed', () => {
    mockedHasAccess.mockReturnValue(true);
    mockedHasRole.mockReturnValue(false);

    render(
      <MemoryRouter initialEntries={['/users']}>
        <Routes>
          <Route
            path="/users"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <div>Users page</div>
              </ProtectedRoute>
            }
          />
          <Route path="/analytics" element={<div>Analytics page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Analytics page')).toBeInTheDocument();
  });
});
