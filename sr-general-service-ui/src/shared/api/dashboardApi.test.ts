import { dashboardApi } from './dashboardApi';
import { requestJson } from './apiClient';

jest.mock('./apiClient', () => ({
  requestJson: jest.fn(),
}));

describe('dashboardApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests metrics', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ totalIncidents: 10 });

    await dashboardApi.getMetrics();

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/dashboard/metrics',
    });
  });

  it('requests report metrics', async () => {
    (requestJson as jest.Mock).mockResolvedValue({
      total: 1,
      totalChange: '+1%',
      new: 1,
      newChange: '0%',
      inProgress: 0,
      inProgressChange: '0%',
      completed: 0,
      completedChange: '0%',
    });

    await dashboardApi.getReportMetrics();

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/dashboard/report-metrics',
    });
  });

  it('builds current weather query with coordinates', async () => {
    (requestJson as jest.Mock).mockResolvedValue({
      weather: { condition: 'clear', temperature: 10, unit: 'C' },
      riskLevel: 'low',
      lastUpdated: 'now',
    });

    await dashboardApi.getCurrent(55.75, 37.61);

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/dashboard/current',
      params: { lat: 55.75, lng: 37.61 },
    });
  });

  it('omits query when coordinates are absent', async () => {
    (requestJson as jest.Mock).mockResolvedValue({
      weather: { condition: 'rain', temperature: 3, unit: 'C' },
      riskLevel: 'medium',
      lastUpdated: 'now',
    });

    await dashboardApi.getCurrent();

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/dashboard/current',
      params: {},
    });
  });

  it('rethrows request errors', async () => {
    (requestJson as jest.Mock).mockRejectedValue(new Error('Request failed with status 500'));

    await expect(dashboardApi.getMetrics()).rejects.toThrow('Request failed with status 500');
  });
});
