export interface InsurerConfiguration {
  insurerId: string;
  insurerName: string;
  insurerCode: string;
  adapterType: string;
  supportsRealTime: boolean;
  supportsBatch: boolean;
  maxBatchSize: number;
  batchSlaHours: number;
  rateLimitPerMinute: number;
  dataFormat: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InsurerCapabilities {
  supportsRealTime: boolean;
  supportsBatch: boolean;
  maxBatchSize: number;
  batchSlaHours: number;
  rateLimitPerMinute: number;
}
