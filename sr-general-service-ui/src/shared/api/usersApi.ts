import { requestJson, requestVoid } from './apiClient';

export interface ManagerUser {
  id: number;
  telegramName: string;
  firstname: string;
  middlename: string;
  lastname: string;
  department: string;
  city: string;
  phone: string;
  email: string;
  walletAddress: string;
  blockchainVerified: boolean;
}

export interface UpdateManagerUserRequest {
  telegramName: string;
  firstname: string;
  middlename: string;
  lastname: string;
  department: string;
  city: string;
  phone: string;
  email: string;
  walletAddress: string;
}

export const usersApi = {
  list: async (): Promise<ManagerUser[]> => {
    return requestJson<ManagerUser[]>({
      method: 'GET',
      url: '/api/manager/users',
    });
  },

  update: async (id: number, payload: UpdateManagerUserRequest): Promise<ManagerUser> => {
    return requestJson<ManagerUser>({
      method: 'PUT',
      url: `/api/manager/users/${id}`,
      data: payload,
    });
  },

  delete: async (id: number): Promise<void> => {
    return requestVoid({
      method: 'DELETE',
      url: `/api/manager/users/${id}`,
    });
  },
};
