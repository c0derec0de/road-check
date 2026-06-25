import { reportsApi } from './reportsApi';
import { requestJson, requestVoid } from './apiClient';

jest.mock('./apiClient', () => ({
  requestJson: jest.fn(),
  requestVoid: jest.fn(),
}));

describe('reportsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests list with query params', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ reports: [], pagination: {}, filters: {} });

    await reportsApi.list({ page: 2, size: 10, status: 'NEW', riskLevel: 'HIGH' });

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/reports',
      params: { page: 2, size: 10, status: 'NEW', riskLevel: 'HIGH' },
    });
  });

  it('requests list without params when they are absent', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ reports: [], pagination: {}, filters: {} });

    await reportsApi.list();

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/reports',
      params: undefined,
    });
  });

  it('requests report by id', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ id: 5 });

    await reportsApi.getById(5);

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/reports/5',
    });
  });

  it('creates report with payload body', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ created: true });
    const payload = {
      policeUserId: 1,
      incidentType: 'ACCIDENT',
      description: 'desc',
    };

    await reportsApi.create(payload);

    expect(requestJson).toHaveBeenCalledWith({
      method: 'POST',
      url: '/api/reports',
      data: payload,
    });
  });

  it('confirms report with moderator comment', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ updated: true });

    await reportsApi.confirm(3, { comment: 'Факт ДТП подтвержден' });

    expect(requestJson).toHaveBeenCalledWith({
      method: 'PUT',
      url: '/api/reports/3/confirm',
      data: { comment: 'Факт ДТП подтвержден' },
    });
  });

  it('declines report', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ updated: true });

    await reportsApi.decline(3);

    expect(requestJson).toHaveBeenCalledWith({
      method: 'PUT',
      url: '/api/reports/3/decline',
    });
  });

  it('deletes manager report successfully', async () => {
    (requestVoid as jest.Mock).mockResolvedValue(undefined);

    await expect(reportsApi.delete(9)).resolves.toBeUndefined();

    expect(requestVoid).toHaveBeenCalledWith({
      method: 'DELETE',
      url: '/api/manager/reports/9',
    });
  });

  it('throws on failed delete', async () => {
    (requestVoid as jest.Mock).mockRejectedValue(new Error('Request failed with status 403'));

    await expect(reportsApi.delete(9)).rejects.toThrow('Request failed with status 403');
  });

  it('throws on failed requestJson calls', async () => {
    (requestJson as jest.Mock).mockRejectedValue(new Error('Request failed with status 500'));

    await expect(reportsApi.getById(1)).rejects.toThrow('Request failed with status 500');
  });
});
