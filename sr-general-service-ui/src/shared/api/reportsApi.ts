import { requestJson, requestVoid } from './apiClient';

export interface ReportListItem {
  id: number;
  title: string;
  address: string;
  description: string;
  status: string;
  riskLevel: string;
  isDangerousZone: boolean;
  username: string;
  date: string;
}

export interface ReportsPagination {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  itemsPerPage: number;
}

export interface ReportsFilters {
  availableStatuses: string[];
  availableRiskLevels: string[];
}

export interface ReportsListResponse {
  reports: ReportListItem[];
  pagination: ReportsPagination;
  filters: ReportsFilters;
}

export interface ReportDetailsResponse {
  id: number;
  title: string;
  address: string;
  description: string;
  status: string;
  riskLevel: string;
  isDangerousZone: boolean;
  user: {
    id: number;
    username: string;
    fullName: string;
    phone: string;
    blockchainVerified: boolean;
  };
  createdAt: string;
  updatedAt: string;
  photos: string[];
  location: {
    lat: number;
    lng: number;
  };
  blockchainTxHash: string;
  blockchainVerified: boolean;
  blockchainBlockNumber: number;
  comments: Array<{
    id: number;
    user: string;
    text: string;
    createdAt: string;
  }>;
}

export interface CreateReportRequest {
  policeUserId: number;
  userId?: number;
  incidentType: string;
  latitude?: number;
  longitude?: number;
  description?: string;
  photosUuid?: string;
  fatalities?: number;
  injuries?: number;
  cause?: string;
}

export interface ConfirmReportRequest {
  comment: string;
}

export const reportsApi = {
  list: async (params?: {
    page?: number;
    size?: number;
    status?: string;
    riskLevel?: string;
  }): Promise<ReportsListResponse> => {
    return requestJson<ReportsListResponse>({
      method: 'GET',
      url: '/api/reports',
      params,
    });
  },

  getById: async (id: number): Promise<ReportDetailsResponse> => {
    return requestJson<ReportDetailsResponse>({
      method: 'GET',
      url: `/api/reports/${id}`,
    });
  },

  create: async (payload: CreateReportRequest): Promise<unknown> => {
    return requestJson<unknown>({
      method: 'POST',
      url: '/api/reports',
      data: payload,
    });
  },

  confirm: async (id: number, payload: ConfirmReportRequest): Promise<unknown> => {
    return requestJson<unknown>({
      method: 'PUT',
      url: `/api/reports/${id}/confirm`,
      data: payload,
    });
  },

  decline: async (id: number): Promise<unknown> => {
    return requestJson<unknown>({
      method: 'PUT',
      url: `/api/reports/${id}/decline`,
    });
  },

  delete: async (id: number): Promise<void> => {
    return requestVoid({
      method: 'DELETE',
      url: `/api/manager/reports/${id}`,
    });
  },
};
