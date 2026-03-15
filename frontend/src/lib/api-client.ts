import type { ProblemDetail } from '@/types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiError extends Error {
  status: number;
  problemDetail: ProblemDetail;

  constructor(status: number, problemDetail: ProblemDetail) {
    super(problemDetail.detail);
    this.name = 'ApiError';
    this.status = status;
    this.problemDetail = problemDetail;
  }
}

export async function request<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let problem: ProblemDetail;
    try {
      problem = await response.json();
    } catch {
      problem = {
        type: 'about:blank',
        title: 'Error',
        status: response.status,
        detail: response.statusText,
      };
    }
    throw new ApiError(response.status, problem);
  }

  if (response.status === 202 || response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
