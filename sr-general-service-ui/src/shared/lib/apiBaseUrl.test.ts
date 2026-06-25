import { getApiBaseUrl } from './apiBaseUrl';

type GlobalWithApiBaseUrl = typeof globalThis & {
  __APP_API_BASE_URL__?: string;
};

describe('getApiBaseUrl', () => {
  afterEach(() => {
    delete (globalThis as GlobalWithApiBaseUrl).__APP_API_BASE_URL__;
  });

  it('returns default url when nothing is configured', () => {
    delete (globalThis as GlobalWithApiBaseUrl).__APP_API_BASE_URL__;
    expect(getApiBaseUrl()).toBe('http://localhost:8080');
  });

  it('returns configured global api base url', () => {
    (globalThis as GlobalWithApiBaseUrl).__APP_API_BASE_URL__ = 'https://api.example.com/';
    expect(getApiBaseUrl()).toBe('https://api.example.com/');
  });
});
