import { authUtils } from './auth';
import { requestWithStatus } from '../api/apiClient';

jest.mock('../api/apiClient', () => ({
  requestWithStatus: jest.fn(),
}));

describe('authUtils', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  it('returns authentication flags based on token and guest state', () => {
    expect(authUtils.isAuthenticated()).toBe(false);
    expect(authUtils.isGuest()).toBe(false);
    expect(authUtils.hasAccess()).toBe(false);

    localStorage.setItem('auth_guest', 'true');
    expect(authUtils.isGuest()).toBe(true);
    expect(authUtils.hasAccess()).toBe(true);

    localStorage.setItem('auth_token', 'token');
    expect(authUtils.isAuthenticated()).toBe(true);
    expect(authUtils.hasAccess()).toBe(true);
  });

  it('returns null when user is missing or malformed', () => {
    expect(authUtils.getCurrentUser()).toBeNull();

    localStorage.setItem('auth_user', 'not-json');
    expect(authUtils.getCurrentUser()).toBeNull();
  });

  it('returns guest user object for guest session', () => {
    localStorage.setItem('auth_guest', 'true');

    expect(authUtils.getCurrentUser()).toEqual({ name: 'Гость' });
  });

  it('detects roles from stored user', () => {
    localStorage.setItem('auth_user', JSON.stringify({ role: 'USER' }));
    expect(authUtils.isUser()).toBe(true);
    expect(authUtils.isModerator()).toBe(false);
    expect(authUtils.hasRole(['USER'])).toBe(true);

    localStorage.setItem('auth_user', JSON.stringify({ role: 'MODERATOR' }));
    expect(authUtils.isUser()).toBe(false);
    expect(authUtils.isModerator()).toBe(true);
    expect(authUtils.hasRole(['USER', 'MODERATOR'])).toBe(true);
  });

  it('stores token and normalized user after successful login', async () => {
    (requestWithStatus as jest.Mock).mockResolvedValue({
      status: 200,
      data: { success: true, message: 'ok', userId: 12, token: 'jwt-token', role: 'USER' },
    });

    const result = await authUtils.login({ login: 'user@example.com', password: 'secret' });

    expect(result.success).toBe(true);
    expect(localStorage.getItem('auth_token')).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem('auth_user') || '{}')).toEqual({
      userId: 12,
      login: 'user@example.com',
      email: 'user@example.com',
      role: 'USER',
    });
  });

  it('stores user full name on successful register', async () => {
    (requestWithStatus as jest.Mock).mockResolvedValue({
      status: 200,
      data: { success: true, message: 'ok', userId: 7, token: 'reg-token', role: 'MODERATOR' },
    });

    await authUtils.register({
      login: 'new-user',
      password: 'password',
      email: 'new@user.dev',
      firstname: 'John',
      lastname: 'Doe',
    });

    expect(localStorage.getItem('auth_token')).toBe('reg-token');
    expect(JSON.parse(localStorage.getItem('auth_user') || '{}')).toEqual({
      userId: 7,
      login: 'new-user',
      email: 'new@user.dev',
      firstname: 'John',
      lastname: 'Doe',
      name: 'John Doe',
      role: 'MODERATOR',
    });
  });

  it('clears session on logout even when request fails', async () => {
    localStorage.setItem('auth_token', 'token');
    localStorage.setItem('auth_user', '{"login":"user"}');
    (requestWithStatus as jest.Mock).mockRejectedValue(new Error('network'));

    await expect(authUtils.logout()).rejects.toThrow('network');
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('auth_user')).toBeNull();
  });
});
