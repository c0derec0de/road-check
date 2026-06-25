import { dangerousZonesApi } from './dangerousZonesApi';
import { requestJson } from './apiClient';

jest.mock('./apiClient', () => ({
  requestJson: jest.fn(),
}));

describe('dangerousZonesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests dangerous zones list', async () => {
    (requestJson as jest.Mock).mockResolvedValue({ zones: [], total: 0 });

    await dangerousZonesApi.list();

    expect(requestJson).toHaveBeenCalledWith({
      method: 'GET',
      url: '/api/analytics/dangerous-zones',
    });
  });

  it('rethrows request errors', async () => {
    (requestJson as jest.Mock).mockRejectedValue(new Error('Request failed with status 500'));

    await expect(dangerousZonesApi.list()).rejects.toThrow('Request failed with status 500');
  });
});
