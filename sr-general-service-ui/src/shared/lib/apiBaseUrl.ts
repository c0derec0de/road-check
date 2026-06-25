type GlobalWithApiBaseUrl = typeof globalThis & {
  __APP_API_BASE_URL__?: string;
};

export const getApiBaseUrl = (): string => {
  const globalApiBaseUrl = (globalThis as GlobalWithApiBaseUrl).__APP_API_BASE_URL__;
  if (globalApiBaseUrl) {
    return globalApiBaseUrl;
  }

  return 'http://localhost:8080';
};
