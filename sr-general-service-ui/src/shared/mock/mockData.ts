import type { StatData, AreaRiskData } from '../types';

export const mockStatsData: StatData[] = [
  {
    title: 'Всего инцидентов',
    value: '1,234',
    change: '+12.5%',
    changeType: 'negative',
    iconType: 'alert',
  },
  {
    title: 'Активных пользователей',
    value: '5,678',
    change: '+8.2%',
    changeType: 'positive',
    iconType: 'users',
  },
  {
    title: 'Рост безопасности',
    value: '15.3%',
    change: '+2.1%',
    changeType: 'positive',
    iconType: 'trending',
  },
  {
    title: 'Опасных участков',
    value: '23',
    change: '-5.2%',
    changeType: 'positive',
    iconType: 'mapPin',
  },
];

export const mockDangerousAreas: AreaRiskData[] = [
  {
    id: '1',
    name: 'Перекресток Ленинский проспект / ул. Мира',
    riskLevel: 'high',
    incidents: 45,
    coordinates: '55.7558° N, 37.6173° E',
  },
  {
    id: '2',
    name: 'Улица Пушкина, участок 100-200м',
    riskLevel: 'high',
    incidents: 38,
    coordinates: '55.7520° N, 37.6156° E',
  },
  {
    id: '3',
    name: 'Проспект Победы, кольцо',
    riskLevel: 'medium',
    incidents: 28,
    coordinates: '55.7580° N, 37.6190° E',
  },
  {
    id: '4',
    name: 'Московское шоссе, км 15-16',
    riskLevel: 'medium',
    incidents: 22,
    coordinates: '55.7500° N, 37.6200° E',
  },
  {
    id: '5',
    name: 'Улица Гагарина, участок 50-150м',
    riskLevel: 'medium',
    incidents: 19,
    coordinates: '55.7450° N, 37.6250° E',
  },
];

