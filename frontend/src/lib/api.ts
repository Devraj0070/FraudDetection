import {
  AuthResponse,
  CsrfResponse,
  DashboardResponse,
  FraudAlert,
  LoginRequest,
  RegisterRequest,
  ReviewRequest,
  TelemetryMetrics,
  TransactionRequest,
  TransactionResponse,
  TransactionSummaryDto,
} from './types';

class ApiClient {
  private csrfToken: string | null = null;
  private csrfHeaderName = 'X-XSRF-TOKEN';

  public async fetchCsrfToken(): Promise<CsrfResponse> {
    try {
      const res = await fetch('/api/auth/csrf', {
        method: 'GET',
        headers: { Accept: 'application/json' },
        credentials: 'include',
      });
      if (res.ok) {
        const data: CsrfResponse = await res.json();
        this.csrfToken = data.token;
        if (data.headerName) {
          this.csrfHeaderName = data.headerName;
        }
        return data;
      }
    } catch (e) {
      console.warn('Failed to fetch CSRF token:', e);
    }
    return { token: '', headerName: 'X-XSRF-TOKEN', parameterName: '_csrf' };
  }

  private async getActiveCsrfToken(): Promise<string> {
    // 1. Try reading cookie directly
    if (typeof document !== 'undefined') {
      const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
      if (match && match[1]) {
        return decodeURIComponent(match[1]);
      }
    }
    // 2. Fall back to cached token or fetch new one
    if (!this.csrfToken) {
      const csrf = await this.fetchCsrfToken();
      return csrf.token;
    }
    return this.csrfToken;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry = false
  ): Promise<T> {
    const method = options.method?.toUpperCase() || 'GET';
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');

    // Add Content-Type if body is present and not set
    if (options.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    // Add CSRF header for mutating requests
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      const token = await this.getActiveCsrfToken();
      if (token) {
        headers.set(this.csrfHeaderName, token);
        headers.set('X-CSRF-TOKEN', token);
      }
    }

    const config: RequestInit = {
      ...options,
      headers,
      credentials: 'include', // essential for JSESSIONID cookie
    };

    let response: Response;
    try {
      response = await fetch(endpoint, config);
    } catch (err: any) {
      throw new Error(err.message || 'Network error communicating with SafePay backend');
    }

    // Handle 403 CSRF expiration retry once
    if (response.status === 403 && !isRetry && ['POST', 'PUT', 'DELETE'].includes(method)) {
      this.csrfToken = null;
      await this.fetchCsrfToken();
      return this.request<T>(endpoint, options, true);
    }

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
      } catch {
        // response was not JSON
      }
      throw new Error(errorMessage);
    }

    return response.json();
  }

  // Auth Endpoints
  public auth = {
    csrf: () => this.fetchCsrfToken(),
    login: (data: LoginRequest) =>
      this.request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    register: (data: RegisterRequest) =>
      this.request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    logout: () =>
      this.request<AuthResponse>('/api/auth/logout', {
        method: 'POST',
      }),
    me: () => this.request<AuthResponse>('/api/auth/me', { method: 'GET' }),
  };

  // Dashboard Endpoints
  public dashboard = {
    get: () => this.request<DashboardResponse>('/api/dashboard', { method: 'GET' }),
  };

  // Transaction Endpoints
  public transactions = {
    create: (data: TransactionRequest, idempotencyKey?: string) => {
      const headers: Record<string, string> = {};
      if (idempotencyKey) {
        headers['Idempotency-Key'] = idempotencyKey;
      }
      return this.request<TransactionResponse>('/api/transactions', {
        method: 'POST',
        headers,
        body: JSON.stringify(data),
      });
    },
    getHistory: () =>
      this.request<TransactionSummaryDto[]>('/api/transactions/history', { method: 'GET' }),
    getById: (id: number | string) =>
      this.request<TransactionResponse>(`/api/transactions/${id}`, { method: 'GET' }),
  };

  // Admin Endpoints
  public admin = {
    getAlerts: () => this.request<FraudAlert[]>('/api/admin/alerts', { method: 'GET' }),
    getPending: () => this.request<any[]>('/api/admin/pending', { method: 'GET' }),
    approve: (id: number | string, notes?: string) =>
      this.request<{ success: boolean; message: string; transactionId: number; status: string }>(
        `/api/admin/transactions/${id}/approve`,
        {
          method: 'POST',
          body: JSON.stringify({ notes: notes || 'Approved by administrator' }),
        }
      ),
    reject: (id: number | string, notes?: string) =>
      this.request<{ success: boolean; message: string; transactionId: number; status: string }>(
        `/api/admin/transactions/${id}/reject`,
        {
          method: 'POST',
          body: JSON.stringify({ notes: notes || 'Rejected by administrator' }),
        }
      ),
  };

  // Telemetry Endpoints
  public telemetry = {
    getMetrics: () => this.request<TelemetryMetrics>('/api/v1/metrics', { method: 'GET' }),
  };
}

export const api = new ApiClient();