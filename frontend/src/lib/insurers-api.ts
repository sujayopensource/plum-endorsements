import { request } from './api-client';
import type {
  InsurerConfiguration,
  InsurerCapabilities,
} from '@/types/insurer-configuration';

export function listInsurers(): Promise<InsurerConfiguration[]> {
  return request('/insurers');
}

export function getInsurer(insurerId: string): Promise<InsurerConfiguration> {
  return request(`/insurers/${insurerId}`);
}

export function getInsurerCapabilities(
  insurerId: string,
): Promise<InsurerCapabilities> {
  return request(`/insurers/${insurerId}/capabilities`);
}
