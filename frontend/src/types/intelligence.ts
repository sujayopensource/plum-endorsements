export interface AnomalyDetection {
  id: string;
  endorsementId: string;
  employerId: string;
  anomalyType: string;
  score: number;
  explanation: string;
  flaggedAt: string;
  reviewedAt: string | null;
  status: string;
  reviewerNotes: string | null;
}

export interface BalanceForecast {
  id: string;
  employerId: string;
  insurerId: string;
  forecastDate: string;
  forecastedAmount: number;
  actualAmount: number | null;
  accuracy: number | null;
  narrative: string;
  createdAt: string;
}

export interface ErrorResolution {
  id: string;
  endorsementId: string;
  errorType: string;
  originalValue: string;
  correctedValue: string;
  resolution: string;
  confidence: number;
  autoApplied: boolean;
  createdAt: string;
}

export interface ErrorResolutionStats {
  totalResolutions: number;
  autoApplied: number;
  suggested: number;
  autoApplyRate: number;
}

export interface ProcessMiningMetric {
  id: string;
  insurerId: string;
  fromStatus: string;
  toStatus: string;
  avgDurationMs: number;
  p95DurationMs: number;
  p99DurationMs: number;
  sampleCount: number;
  happyPathPct: number;
  calculatedAt: string;
}

export interface ProcessMiningInsight {
  insurerId: string;
  insurerName: string;
  insightType: string;
  insight: string;
  calculatedAt: string;
}

export interface StpRate {
  overallStpRate: number;
  perInsurerStpRate: Record<string, number>;
}
