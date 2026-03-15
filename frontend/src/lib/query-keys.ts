export const queryKeys = {
  endorsements: {
    all: ['endorsements'] as const,
    lists: () => [...queryKeys.endorsements.all, 'list'] as const,
    list: (filters: Record<string, unknown>) =>
      [...queryKeys.endorsements.lists(), filters] as const,
    details: () => [...queryKeys.endorsements.all, 'detail'] as const,
    detail: (id: string) =>
      [...queryKeys.endorsements.details(), id] as const,
    coverage: (id: string) =>
      [...queryKeys.endorsements.detail(id), 'coverage'] as const,
    batches: () => [...queryKeys.endorsements.all, 'batches'] as const,
    batchList: (filters: Record<string, unknown>) =>
      [...queryKeys.endorsements.batches(), filters] as const,
    outstanding: () => [...queryKeys.endorsements.all, 'outstanding'] as const,
    outstandingList: (filters: Record<string, unknown>) =>
      [...queryKeys.endorsements.outstanding(), filters] as const,
  },
  eaAccounts: {
    all: ['ea-accounts'] as const,
    detail: (employerId: string, insurerId: string) =>
      [...queryKeys.eaAccounts.all, employerId, insurerId] as const,
  },
  insurers: {
    all: ['insurers'] as const,
    lists: () => [...queryKeys.insurers.all, 'list'] as const,
    details: () => [...queryKeys.insurers.all, 'detail'] as const,
    detail: (insurerId: string) =>
      [...queryKeys.insurers.details(), insurerId] as const,
    capabilities: (insurerId: string) =>
      [...queryKeys.insurers.detail(insurerId), 'capabilities'] as const,
  },
  reconciliation: {
    all: ['reconciliation'] as const,
    runs: () => [...queryKeys.reconciliation.all, 'runs'] as const,
    runsByInsurer: (insurerId: string) =>
      [...queryKeys.reconciliation.runs(), insurerId] as const,
    items: (runId: string) =>
      [...queryKeys.reconciliation.all, 'items', runId] as const,
  },
};
