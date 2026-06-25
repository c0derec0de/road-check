import type { AIRiskRecommendationData, PatrolPointData } from '../types';

export const aiRiskRecommendation: AIRiskRecommendationData = {
  title: 'Рекомендация на сегодня',
  recommendations: [
    'Усилить патрулирование в районе проспекта Ленина из-за прогнозируемого гололеда',
    'Обратить внимание на перекрестки в час пик (08:00-10:00 и 17:00-19:00)',
    'Проверить состояние дорожного покрытия на участках с высоким уровнем риска',
  ],
};

export const mockPatrolPoints: PatrolPointData[] = [
  {
    id: '1',
    number: 1,
    address: 'Проспект Ленина, перекресток с ул. Мира',
    time: '08:00-10:00',
    reason: 'Гололед + час пик',
    riskLevel: 'high',
    riskPercentage: 95,
  },
  {
    id: '2',
    number: 2,
    address: 'Улица Пушкина, участок 100-200м',
    time: '12:00-14:00',
    reason: 'Высокая интенсивность движения',
    riskLevel: 'high',
    riskPercentage: 87,
  },
  {
    id: '3',
    number: 3,
    address: 'Проспект Победы, кольцо',
    time: '17:00-19:00',
    reason: 'Час пик',
    riskLevel: 'medium',
    riskPercentage: 65,
  },
];

