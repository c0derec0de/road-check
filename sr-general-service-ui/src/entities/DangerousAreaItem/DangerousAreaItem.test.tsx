import { render, screen } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { DangerousAreaItem } from './DangerousAreaItem';
import type { AreaRiskData } from '../../shared/types';

const renderWithMantine = (area: AreaRiskData) => {
  return render(
    <MantineProvider>
      <DangerousAreaItem area={area} />
    </MantineProvider>
  );
};

describe('DangerousAreaItem', () => {
  it('clamps trend index to minimum value (10%)', () => {
    const area: AreaRiskData = {
      id: '1',
      name: 'Test area',
      riskLevel: 'low',
      incidents: 0,
      coordinates: '55.7558, 37.6173',
    };

    renderWithMantine(area);

    expect(screen.getByText('Test area')).toBeInTheDocument();
    expect(screen.getByText(/10%/)).toBeInTheDocument();
  });

  it('clamps trend index to maximum value (100%)', () => {
    const area: AreaRiskData = {
      id: '2',
      name: 'High risk area',
      riskLevel: 'high',
      incidents: 1000,
      coordinates: '59.9343, 30.3351',
    };

    renderWithMantine(area);

    expect(screen.getByText('High risk area')).toBeInTheDocument();
    expect(screen.getByText(/100%/)).toBeInTheDocument();
  });
});
