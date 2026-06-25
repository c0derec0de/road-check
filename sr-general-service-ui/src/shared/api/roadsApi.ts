import { requestJson, requestVoid } from './apiClient';

export interface ManagerRoad {
  id: number;
  name: string;
  regionId?: number | null;
  status?: string | null;
  lengthKm?: number | null;
  address?: string | null;
  description?: string | null;
}

export interface ManagerRoadPayload {
  name: string;
  regionId?: number;
  status?: string;
  lengthKm?: number;
  address?: string;
  description?: string;
}

export const roadsApi = {
  list: async (): Promise<ManagerRoad[]> => {
    return requestJson<ManagerRoad[]>({
      method: 'GET',
      url: '/api/manager/roads',
    });
  },

  getById: async (id: number): Promise<ManagerRoad> => {
    return requestJson<ManagerRoad>({
      method: 'GET',
      url: `/api/manager/roads/${id}`,
    });
  },

  create: async (payload: ManagerRoadPayload): Promise<ManagerRoad> => {
    return requestJson<ManagerRoad>({
      method: 'POST',
      url: '/api/manager/roads',
      data: payload,
    });
  },

  update: async (id: number, payload: ManagerRoadPayload): Promise<ManagerRoad> => {
    return requestJson<ManagerRoad>({
      method: 'PUT',
      url: `/api/manager/roads/${id}`,
      data: payload,
    });
  },

  delete: async (id: number): Promise<void> => {
    return requestVoid({
      method: 'DELETE',
      url: `/api/manager/roads/${id}`,
    });
  },
};
