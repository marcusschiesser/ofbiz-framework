import type { ApiError } from '@/api/types';

const BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export class ApiClientError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let fallback = `Request failed with status ${response.status}`;
    try {
      const body = (await response.json()) as Partial<ApiError>;
      if (typeof body.message === 'string') {
        fallback = body.message;
      }
    } catch {
      // ignore parse failures
    }
    throw new ApiClientError(response.status, fallback);
  }

  return (await response.json()) as T;
}
