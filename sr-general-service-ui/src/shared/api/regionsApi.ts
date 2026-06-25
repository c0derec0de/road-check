import { requestJson, requestVoid } from "./apiClient";

export interface Region {
  id: number;
  name: string;
  code?: string | null;
  city?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  description?: string | null;
}

export interface RegionWeather {
  condition?: string;
  temperature?: number;
  unit?: string;
  riskLevel?: string;
  windSpeedKmh?: number;
  lastUpdated?: string;
}

export interface ManagerRegionPayload {
  name: string;
  code?: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  description?: string;
}

export const regionsApi = {
  list: async (): Promise<Region[]> => {
    return requestJson<Region[]>({
      method: "GET",
      url: "/api/regions",
    });
  },

  getById: async (id: number): Promise<Region> => {
    return requestJson<Region>({
      method: "GET",
      url: `/api/regions/${id}`,
    });
  },

  getWeather: async (id: number): Promise<RegionWeather> => {
    return requestJson<RegionWeather>({
      method: "GET",
      url: `/api/regions/${id}/weather`,
    });
  },
};

export const managerRegionsApi = {
  list: async (): Promise<Region[]> => {
    return requestJson<Region[]>({
      method: "GET",
      url: "/api/manager/regions",
    });
  },

  getById: async (id: number): Promise<Region> => {
    return requestJson<Region>({
      method: "GET",
      url: `/api/manager/regions/${id}`,
    });
  },

  create: async (payload: ManagerRegionPayload): Promise<Region> => {
    return requestJson<Region>({
      method: "POST",
      url: "/api/manager/regions",
      data: payload,
    });
  },

  update: async (
    id: number,
    payload: ManagerRegionPayload,
  ): Promise<Region> => {
    return requestJson<Region>({
      method: "PUT",
      url: `/api/manager/regions/${id}`,
      data: payload,
    });
  },

  delete: async (id: number): Promise<void> => {
    return requestVoid({
      method: "DELETE",
      url: `/api/manager/regions/${id}`,
    });
  },
};
