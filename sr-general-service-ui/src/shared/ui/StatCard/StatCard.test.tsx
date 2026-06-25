import { render, screen } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import type { ReactElement } from 'react';
import { StatCard } from './StatCard';
import type { StatData } from '../../types';

const renderWithMantine = (ui: ReactElement) => {
  return render(<MantineProvider>{ui}</MantineProvider>);
};

describe('StatCard', () => {
  it('renders title and value', () => {
    const data: StatData = {
      title: 'Total incidents',
      value: '120',
      change: '+5%',
      changeType: 'positive',
      iconType: 'alert',
    };

    renderWithMantine(<StatCard data={data} />);

    expect(screen.getByText('Total incidents')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
  });

  it('formats percent change to a readable compact value', () => {
    const data: StatData = {
      title: 'Safety growth',
      value: '78%',
      change: '+12.3%',
      changeType: 'positive',
      iconType: 'trending',
    };

    renderWithMantine(<StatCard data={data} />);

    expect(screen.getByText(/^\+12%/)).toBeInTheDocument();
  });

  it('renders negative change and icon for negative type', () => {
    const data: StatData = {
      title: 'Danger zones',
      value: '17',
      change: '-2%',
      changeType: 'negative',
      iconType: 'mapPin',
    };

    const { container } = renderWithMantine(<StatCard data={data} />);

    expect(screen.getByText(/^-2\.0%/)).toBeInTheDocument();
    expect(container.querySelector('svg')).not.toBeNull();
  });

  it('keeps non-percent change as is and hides icon when iconType is absent', () => {
    const data: StatData = {
      title: 'Neutral trend',
      value: 'N/A',
      change: 'stable',
      changeType: 'neutral',
    };

    const { container } = renderWithMantine(<StatCard data={data} />);

    expect(screen.getByText(/^stable/)).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeNull();
  });
});
