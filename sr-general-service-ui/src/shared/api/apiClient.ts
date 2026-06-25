import axios, { AxiosHeaders, type AxiosRequestConfig } from 'axios';
import { getApiBaseUrl } from '../lib/apiBaseUrl';

const normalizeBaseUrl = (baseUrl: string): string => {
  if (!baseUrl) {
    return '';
  }

  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
};

export const apiClient = axios.create({
  headers: {
    'Content-Type': 'application/json',
    'X-From-Nginx': '1',
  },
});

export class ApiRequestError extends Error {
  status?: number;
  responseMessage?: string;

  constructor(message: string, options?: { status?: number; responseMessage?: string }) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = options?.status;
    this.responseMessage = options?.responseMessage;
  }
}

const getResponseMessage = (data: unknown): string | undefined => {
  if (!data || typeof data !== 'object' || !('message' in data)) {
    return undefined;
  }

  const message = (data as { message?: unknown }).message;
  return typeof message === 'string' ? message : undefined;
};

apiClient.interceptors.request.use((config) => {
  const token = globalThis.localStorage?.getItem('auth_token');
  const headers = AxiosHeaders.from(config.headers ?? {});
  const runtimeBaseUrl = normalizeBaseUrl(getApiBaseUrl());
  headers.set('X-From-Nginx', '1');

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  } else {
    headers.delete('Authorization');
  }

  config.baseURL = runtimeBaseUrl;
  config.headers = headers;
  return config;
});

const toRequestError = (error: unknown): Error => {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    if (status) {
      const responseMessage = getResponseMessage(error.response?.data);
      const message = responseMessage
        ? `Request failed with status ${status}: ${responseMessage}`
        : `Request failed with status ${status}`;

      return new ApiRequestError(message, { status, responseMessage });
    }

    if (error.request) {
      const targetUrl = error.config?.url ?? 'unknown url';
      return new ApiRequestError(`Network request failed for ${targetUrl}`);
    }
  }

  if (error instanceof Error) {
    return error;
  }

  return new ApiRequestError('Request failed');
};

export const requestJson = async <T>(config: AxiosRequestConfig): Promise<T> => {
  try {
    const response = await apiClient.request<T>(config);
    return response.data;
  } catch (error) {
    throw toRequestError(error);
  }
};

export const requestVoid = async (config: AxiosRequestConfig): Promise<void> => {
  try {
    await apiClient.request(config);
  } catch (error) {
    throw toRequestError(error);
  }
};

export const requestBlob = async (config: AxiosRequestConfig): Promise<Blob> => {
  try {
    const response = await apiClient.request<Blob>({
      ...config,
      responseType: 'blob',
    });
    return response.data;
  } catch (error) {
    throw toRequestError(error);
  }
};

export const requestWithStatus = async <T>(config: AxiosRequestConfig): Promise<{
  data: T;
  status: number;
}> => {
  try {
    const response = await apiClient.request<T>({
      validateStatus: () => true,
      ...config,
    });

    return {
      data: response.data,
      status: response.status,
    };
  } catch (error) {
    throw toRequestError(error);
  }
};
