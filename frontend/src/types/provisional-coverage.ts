export interface ProvisionalCoverage {
  id: string;
  endorsementId: string;
  employeeId: string;
  employerId: string;
  coverageStart: string;
  coverageType: 'PROVISIONAL' | 'CONFIRMED';
  confirmedAt: string | null;
  expiredAt: string | null;
  createdAt: string;
  active: boolean;
}
