import { getApiBaseUrl } from './apiBaseUrl';
import { requestWithStatus } from '../api/apiClient';

const AUTH_TOKEN_KEY = 'auth_token';
const AUTH_USER_KEY = 'auth_user';
const AUTH_GUEST_KEY = 'auth_guest';

export interface AuthUser {
  userId?: number;
  login?: string;
  email?: string;
  name?: string;
  firstname?: string;
  lastname?: string;
  role?: string;
}

export type AuthRole = 'USER' | 'MODERATOR';

export interface LoginRequest {
  login: string;
  password: string;
}

export interface RegisterRequest {
  login: string;
  password: string;
  email?: string;
  firstname?: string;
  lastname?: string;
  phone?: string;
  walletAddress?: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  userId?: number;
  token?: string;
  blockchainVerified?: boolean;
  role?: string;
}

const saveSession = (user: AuthUser, token: string): void => {
  localStorage.removeItem(AUTH_GUEST_KEY);
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
};

const clearSession = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  localStorage.removeItem(AUTH_GUEST_KEY);
};

const getStoredToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY);
};

const isGuestSession = (): boolean => {
  return localStorage.getItem(AUTH_GUEST_KEY) === 'true';
};

const authUrl = (path: string): string => {
  const apiBaseUrl = getApiBaseUrl();

  if (!apiBaseUrl) {
    return `/api/auth${path}`;
  }

  return `${apiBaseUrl.endsWith('/') ? apiBaseUrl.slice(0, -1) : apiBaseUrl}/api/auth${path}`;
};

export const authUtils = {
  isAuthenticated: (): boolean => {
    return !!getStoredToken();
  },

  isGuest: (): boolean => {
    return isGuestSession();
  },

  hasAccess: (): boolean => {
    return !!getStoredToken() || isGuestSession();
  },

  enterGuest: (): void => {
    clearSession();
    localStorage.setItem(AUTH_GUEST_KEY, 'true');
  },

  getToken: (): string | null => {
    return getStoredToken();
  },

  getCurrentUser: (): AuthUser | null => {
    if (isGuestSession()) {
      return { name: 'Гость' };
    }
    const userStr = localStorage.getItem(AUTH_USER_KEY);
    if (!userStr) {
      return null;
    }

    try {
      return JSON.parse(userStr) as AuthUser;
    } catch {
      return null;
    }
  },

  isModerator: (): boolean => {
    const user = authUtils.getCurrentUser();
    return user?.role?.toUpperCase() === 'MODERATOR';
  },

  isUser: (): boolean => {
    const user = authUtils.getCurrentUser();
    return user?.role?.toUpperCase() === 'USER';
  },

  hasRole: (roles: AuthRole[]): boolean => {
    const userRole = authUtils.getCurrentUser()?.role?.toUpperCase();
    if (!userRole) {
      return false;
    }

    return roles.includes(userRole as AuthRole);
  },

  login: async (payload: LoginRequest): Promise<AuthResponse> => {
    const { data, status } = await requestWithStatus<AuthResponse>({
      method: 'POST',
      url: authUrl('/login'),
      data: payload,
    });

    if (status >= 200 && status < 300 && data.success && data.token) {
      const user: AuthUser = {
        userId: data.userId,
        login: payload.login,
        email: payload.login.includes('@') ? payload.login : undefined,
        role: data.role,
      };
      saveSession(user, data.token);
    }

    return data;
  },

  register: async (payload: RegisterRequest): Promise<AuthResponse> => {
    const { data, status } = await requestWithStatus<AuthResponse>({
      method: 'POST',
      url: authUrl('/register'),
      data: payload,
    });

    if (status >= 200 && status < 300 && data.success && data.token) {
      const fullName = [payload.firstname, payload.lastname].filter(Boolean).join(' ');
      const user: AuthUser = {
        userId: data.userId,
        login: payload.login,
        email: payload.email,
        firstname: payload.firstname,
        lastname: payload.lastname,
        name: fullName || undefined,
        role: data.role,
      };
      saveSession(user, data.token);
    }

    return data;
  },

  logout: async (): Promise<AuthResponse> => {
    try {
      const { data } = await requestWithStatus<AuthResponse>({
        method: 'POST',
        url: authUrl('/logout'),
      });
      return data;
    } finally {
      clearSession();
    }
  },
};
