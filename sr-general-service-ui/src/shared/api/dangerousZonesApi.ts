import { requestJson } from './apiClient';

export interface DangerousZone {
  id: number;
  name: string;
  incidents: number;
  riskLevel: string;
  coordinates: {
    lat: number;
    lng: number;
  };
}

export interface DangerousZonesResponse {
  zones: DangerousZone[];
  total: number;
}

export const dangerousZonesApi = {
  list: async (): Promise<DangerousZonesResponse> => {
    return requestJson<DangerousZonesResponse>({
      method: 'GET',
      url: '/api/analytics/dangerous-zones',
    });
  },
};
