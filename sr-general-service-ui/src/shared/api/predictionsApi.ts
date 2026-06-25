import { requestBlob } from './apiClient';

export const predictionsApi = {
  getRiskChart: (): Promise<Blob> =>
    requestBlob({
      method: 'GET',
      url: '/api/predictions/charts/risk',
      headers: { Accept: 'image/svg+xml' },
    }),
  getMonthlyChart: (): Promise<Blob> =>
    requestBlob({
      method: 'GET',
      url: '/api/predictions/charts/monthly',
      headers: { Accept: 'image/svg+xml' },
    }),
  getCausesChart: (): Promise<Blob> =>
    requestBlob({
      method: 'GET',
      url: '/api/predictions/charts/causes',
      headers: { Accept: 'image/svg+xml' },
    }),
};
