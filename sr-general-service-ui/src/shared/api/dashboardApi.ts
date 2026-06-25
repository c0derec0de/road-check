import { requestJson } from './apiClient';

export interface DashboardMetricsResponse {
  totalIncidents: number;
  incidentsChange: string;
  activeUsers: number;
  usersChange: string;
  safetyGrowth: number;
  safetyChange: string;
  dangerousZones: number;
  zonesChange: string;
}

export interface ReportMetricsResponse {
  total: number;
  totalChange: string;
  new: number;
  newChange: string;
  inProgress: number;
  inProgressChange: string;
  completed: number;
  completedChange: string;
}

export interface CurrentWeather {
  condition: string;
  temperature: number;
  unit: string;
}

export interface CurrentWeatherResponse {
  weather: CurrentWeather;
  riskLevel: string;
  lastUpdated: string;
}

export const dashboardApi = {
  getMetrics: (): Promise<DashboardMetricsResponse> => {
    return requestJson<DashboardMetricsResponse>({
      method: 'GET',
      url: '/api/dashboard/metrics',
    });
  },

  getReportMetrics: (): Promise<ReportMetricsResponse> => {
    return requestJson<ReportMetricsResponse>({
      method: 'GET',
      url: '/api/dashboard/report-metrics',
    });
  },

  getCurrent: (lat?: number, lng?: number): Promise<CurrentWeatherResponse> => {
    return requestJson<CurrentWeatherResponse>({
      method: 'GET',
      url: '/api/dashboard/current',
      params: {
        ...(lat !== undefined ? { lat } : {}),
        ...(lng !== undefined ? { lng } : {}),
      },
    });
  },
};
