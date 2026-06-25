import { predictionsApi } from './predictionsApi';
import { requestBlob } from './apiClient';

jest.mock('./apiClient', () => ({
  requestBlob: jest.fn(),
}));

describe('predictionsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests risk chart', async () => {
    (requestBlob as jest.Mock).mockResolvedValue(new Blob());

    await predictionsApi.getRiskChart();

    expect(requestBlob).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/predictions/charts/risk',
      headers: { Accept: 'image/svg+xml' },
    });
  });

  it('requests monthly chart', async () => {
    (requestBlob as jest.Mock).mockResolvedValue(new Blob());

    await predictionsApi.getMonthlyChart();

    expect(requestBlob).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/predictions/charts/monthly',
      headers: { Accept: 'image/svg+xml' },
    });
  });

  it('returns causes chart blob', async () => {
    const blob = new Blob(['causes']);
    (requestBlob as jest.Mock).mockResolvedValue(blob);

    await expect(predictionsApi.getCausesChart()).resolves.toBe(blob);
  });

  it('rethrows request errors', async () => {
    (requestBlob as jest.Mock).mockRejectedValue(new Error('Request failed with status 404'));

    await expect(predictionsApi.getRiskChart()).rejects.toThrow('Request failed with status 404');
  });
});
