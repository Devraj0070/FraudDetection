export interface CsrfResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  username: string | null;
  email: string | null;
  role: string | null;
  accountNumber: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface TransactionSummaryDto {
  id: number;
  amount: number;
  transactionType: string;
  status: 'APPROVED' | 'PENDING_REVIEW' | 'DECLINED' | 'BLOCKED' | string;
  transactionTime: string;
  accountNumber?: string;
  customerUsername?: string;
}

export interface DashboardResponse {
  success: boolean;
  message: string;
  username: string;
  role: string;
  accountNumber: string;
  accountType: string;
  accountStatus: string;
  balance: number;
  totalTransactions: number;
  approvedTransactions: number;
  declinedTransactions: number;
  blockedTransactions: number;
  recentTransactions: TransactionSummaryDto[];
}

export type TransactionType = 'PAYMENT' | 'TRANSFER' | 'CASH_OUT' | 'DEBIT' | 'CASH_IN';

export interface TransactionRequest {
  amount: number;
  transactionType: TransactionType;
  receiverAccountNumber?: string;
  idempotencyKey?: string;
}

export interface TransactionResponse {
  success: boolean;
  message: string;
  transactionId: number | null;
  amount: number | null;
  transactionType: string | null;
  status: 'APPROVED' | 'PENDING_REVIEW' | 'DECLINED' | 'BLOCKED' | string;
  transactionTime: string | null;
  detectionReason: string | null;
  riskAssessment: 'LOW' | 'MEDIUM' | 'HIGH' | string;
  fraudProbability: number | null;
  newBalance: number | null;
}

export interface FraudAlert {
  id: number;
  alertType: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  status: 'OPEN' | 'RESOLVED' | string;
  createdAt: string;
  decision?: string;
  reviewNotes?: string;
  reviewedAt?: string;
  transaction?: {
    id: number;
    amount: number;
    transactionType: string;
    status: string;
    transactionTime: string;
    account?: {
      accountNumber: string;
      user?: {
        username: string;
      };
    };
    receiverAccount?: {
      accountNumber: string;
      user?: {
        username: string;
      };
    };
  };
  reviewingAdmin?: {
    username: string;
  };
}

export interface ReviewRequest {
  notes?: string;
}

export interface TelemetryMetrics {
  service: string;
  status: string;
  servlet?: string;
  uptimeSeconds: number;
  heapMemoryUsedMb?: number;
  heapMemoryMaxMb?: number;
  jdbcDatabaseConnected: boolean;
  dbConnected?: boolean;
  engine?: {
    model: string;
    ruleEngine: string;
    riskScoring: string;
  };
}
