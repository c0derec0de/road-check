import { render, screen } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { DangerousAreasList } from './DangerousAreasList';
import type { AreaRiskData } from '../../shared/types';

describe('DangerousAreasList', () => {
  it('renders all passed dangerous areas and top badge value', () => {
    const areas: AreaRiskData[] = [
      {
        id: '1',
        name: 'Area 1',
        riskLevel: 'high',
        incidents: 24,
        coordinates: '55.75, 37.61',
      },
      {
        id: '2',
        name: 'Area 2',
        riskLevel: 'medium',
        incidents: 12,
        coordinates: '59.93, 30.33',
      },
      {
        id: '3',
        name: 'Area 3',
        riskLevel: 'low',
        incidents: 4,
        coordinates: '56.84, 60.61',
      },
    ];

    render(
      <MantineProvider>
        <DangerousAreasList areas={areas} />
      </MantineProvider>
    );

    expect(screen.getByText('Area 1')).toBeInTheDocument();
    expect(screen.getByText('Area 2')).toBeInTheDocument();
    expect(screen.getByText('Area 3')).toBeInTheDocument();
    expect(screen.getByText('Топ-3')).toBeInTheDocument();
  });
});
