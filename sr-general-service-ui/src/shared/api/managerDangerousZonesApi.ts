import { requestJson, requestVoid } from './apiClient';

export interface ManagerDangerousZone {
  id: number;
  name: string;
  regionId?: number | null;
  incidents?: number | null;
  riskLevel?: string | null;
  description?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  active?: boolean | null;
}

export interface ManagerDangerousZonePayload {
  name: string;
  regionId?: number;
  incidents?: number;
  riskLevel?: string;
  description?: string;
  latitude?: number;
  longitude?: number;
  active?: boolean;
}

export const managerDangerousZonesApi = {
  list: async (): Promise<ManagerDangerousZone[]> => {
    return requestJson<ManagerDangerousZone[]>({
      method: 'GET',
      url: '/api/manager/dangerous-zones',
    });
  },

  getById: async (id: number): Promise<ManagerDangerousZone> => {
    return requestJson<ManagerDangerousZone>({
      method: 'GET',
      url: `/api/manager/dangerous-zones/${id}`,
    });
  },

  create: async (payload: ManagerDangerousZonePayload): Promise<ManagerDangerousZone> => {
    return requestJson<ManagerDangerousZone>({
      method: 'POST',
      url: '/api/manager/dangerous-zones',
      data: payload,
    });
  },

  update: async (id: number, payload: ManagerDangerousZonePayload): Promise<ManagerDangerousZone> => {
    return requestJson<ManagerDangerousZone>({
      method: 'PUT',
      url: `/api/manager/dangerous-zones/${id}`,
      data: payload,
    });
  },

  delete: async (id: number): Promise<void> => {
    return requestVoid({
      method: 'DELETE',
      url: `/api/manager/dangerous-zones/${id}`,
    });
  },
};
