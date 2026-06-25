export type IconType = 'alert' | 'users' | 'trending' | 'mapPin';

export interface StatData {
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative' | 'neutral';
  iconType?: IconType;
  hint?: string;
}

export interface AreaRiskData {
  id: string;
  name: string;
  riskLevel: 'high' | 'medium' | 'low';
  incidents: number;
  coordinates: string;
}

export type AppealStatus = 'new' | 'in_progress' | 'completed' | 'declined';
export type AppealPriority = 'high' | 'medium' | 'low';

export type AppealType = 'infrastructure' | 'traffic' | 'safety' | 'other';

export interface AppealData {
  id: string;
  title: string;
  description: string;
  author: string;
  date: string;
  status: AppealStatus;
  priority: AppealPriority;
  isDangerousArea: boolean;
  photosCount: number;
  answer?: string;
  type?: AppealType;
  location?: string;
  dateTime?: string;
  assignedTo?: string;
  attachments?: string[];
}

export interface AppealSummaryData {
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative' | 'neutral';
  iconType?: IconType;
  hint?: string;
}

export interface AIRiskRecommendationData {
  title: string;
  recommendations: string[];
}

export interface PatrolPointData {
  id: string;
  number: number;
  address: string;
  time: string;
  reason: string;
  riskLevel: 'high' | 'medium' | 'low';
  riskPercentage: number;
}
