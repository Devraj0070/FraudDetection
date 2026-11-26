'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { DashboardResponse, TransactionResponse, TransactionType } from '@/lib/types';
import { CustomerNavbar } from '@/components/layout/CustomerNavbar';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/Badge';
import { formatCurrency } from '@/lib/utils';
import {
  Send,
  AlertCircle,
  CheckCircle2,
  AlertTriangle,
  Lock,
  ArrowRight,
  Shield,
  Cpu,
  ChevronRight,
  UserCheck,
  Zap,
} from 'lucide-react';

export default function TransferPage() {
  const { user, loading: authLoading } = useAuth();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [receiverAccount, setReceiverAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [transactionType, setTransactionType] = useState<TransactionType>('TRANSFER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TransactionResponse | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!authLoading && !user) {
      router.push('/login');
      return;
    }
    if (user) {
      api.dashboard.get().then((data) => setDashboard(data)).catch(() => {});
    }
  }, [user, authLoading, router]);

  const handleSelectRecipient = (acct: string) => {
    setReceiverAccount(acct);
    setError(null);
  };

  const handleSelectAmount = (val: number | string) => {
    setAmount(val.toString());
    setError(null);
  };

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please specify a positive transfer amount greater than 0.');
      return;
    }

    if ((transactionType === 'TRANSFER' || transactionType === 'PAYMENT') && !receiverAccount.trim()) {
      setError('Recipient account number is mandatory for transfer and payment operations.');
      return;
    }

    // Generate unique idempotency key for this logical submission
    const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'txn_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);

    setLoading(true);
    try {
      const res = await api.transactions.create(
        {
          amount: parsedAmount,
          transactionType,
          receiverAccountNumber: receiverAccount.trim() || undefined,
          idempotencyKey,
        },
        idempotencyKey
      );

      setResult(res);
      setIsModalOpen(true);
      if (res.success) {
        setAmount('');
        // Refresh dashboard balance
        api.dashboard.get().then((data) => setDashboard(data)).catch(() => {});
      }
    } catch (err: any) {
      setError(err.message || 'Transaction submission failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F3F3F3] text-[#111111] flex flex-col selection:bg-lime selection:text-black">
      <CustomerNavbar />

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="w-2 h-2 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
              <span className="font-mono text-xs uppercase tracking-widest text-neutral-600 font-semibold">
                PAYMENTS & SETTLEMENT GATEWAY
              </span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl uppercase font-bold text-black tracking-tight">
              Transfer Funds
            </h1>
          </div>
          <Link
            href="/dashboard"
            className="font-mono text-xs text-neutral-600 hover:text-black hover:underline"
          >
            ← Back to Vault
          </Link>
        </div>

        {/* Sender Vault Balance Summary Pill */}
        <div className="mb-6 p-4 rounded-2xl bg-white border border-neutral-200/80 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-black text-lime flex items-center justify-center font-bold">
              <Lock className="w-4 h-4" />
            </div>
            <div>
              <div className="font-mono text-[11px] uppercase tracking-wider text-neutral-500 font-semibold">
                Debited Account Vault
              </div>
              <div className="font-mono text-xs font-bold text-black">
                {user?.username} ({user?.accountNumber || dashboard?.accountNumber || 'Primary'})
              </div>
            </div>
          </div>
          <div className="text-left sm:text-right">
            <div className="font-mono text-[11px] uppercase tracking-wider text-neutral-500 font-semibold">
              Available Balance
            </div>
            <div className="font-display text-2xl font-bold text-black">
              {formatCurrency(dashboard?.balance ?? 0)}
            </div>
          </div>
        </div>

        {/* Transfer Form Card */}
        <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-sm p-6 sm:p-8">
          {error && (
            <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-mono flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleTransfer} className="space-y-6">
            {/* Transaction Type */}
            <div>
              <label className="block font-mono text-xs uppercase tracking-wider text-neutral-700 font-semibold mb-2">
                Operation Type
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {(['TRANSFER', 'PAYMENT', 'CASH_OUT', 'DEBIT', 'CASH_IN'] as TransactionType[]).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setTransactionType(type)}
                    className={
                      transactionType === type
                        ? 'p-3 rounded-xl border-2 border-black bg-black text-lime font-mono text-xs font-semibold flex items-center justify-center transition-all'
                        : 'p-3 rounded-xl border border-neutral-200 bg-neutral-50 text-neutral-700 hover:bg-neutral-100 font-mono text-xs font-medium flex items-center justify-center transition-all'
                    }
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            {/* Counterparty Account */}
            {(transactionType === 'TRANSFER' || transactionType === 'PAYMENT') && (
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="block font-mono text-xs uppercase tracking-wider text-neutral-700 font-semibold">
                    Recipient Account Number
                  </label>
                  <span className="font-mono text-[11px] text-neutral-500">Quick Demo Presets:</span>
                </div>

                {/* Quick Presets for Hackathon Demos */}
                <div className="flex flex-wrap gap-2 mb-2">
                  <button
                    type="button"
                    onClick={() => handleSelectRecipient('100000000002')}
                    className="px-2.5 py-1 rounded-lg bg-neutral-100 hover:bg-neutral-200 text-neutral-800 font-mono text-[11px] border border-neutral-200 flex items-center gap-1 transition-colors"
                  >
                    <UserCheck className="w-3 h-3 text-neutral-600" />
                    <span>Demo User (100000000002)</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSelectRecipient('100000000001')}
                    className="px-2.5 py-1 rounded-lg bg-neutral-100 hover:bg-neutral-200 text-neutral-800 font-mono text-[11px] border border-neutral-200 flex items-center gap-1 transition-colors"
                  >
                    <Shield className="w-3 h-3 text-neutral-600" />
                    <span>Admin Corporate (100000000001)</span>
                  </button>
                </div>

                <input
                  type="text"
                  value={receiverAccount}
                  onChange={(e) => setReceiverAccount(e.target.value)}
                  placeholder="e.g. 100000000002"
                  required
                  className="w-full px-4 py-3 rounded-xl bg-neutral-50 border border-neutral-200 text-black font-mono text-sm focus:outline-none focus:border-black focus:ring-1 focus:ring-black transition-all"
                />
                <p className="mt-1 font-mono text-[11px] text-neutral-500">
                  Target account must be an active registered SafePay ledger entry.
                </p>
              </div>
            )}

            {/* Transfer Amount */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block font-mono text-xs uppercase tracking-wider text-neutral-700 font-semibold">
                  Amount (INR)
                </label>
                <div className="flex items-center gap-1.5 font-mono text-[11px]">
                  <span className="text-neutral-500">Presets:</span>
                  {[500, 1000, 5000].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => handleSelectAmount(preset)}
                      className="px-2 py-0.5 rounded bg-neutral-100 hover:bg-neutral-200 text-neutral-800 border border-neutral-200"
                    >
                      ₹{preset}
                    </button>
                  ))}
                  {dashboard?.balance && dashboard.balance > 0 && (
                    <button
                      type="button"
                      onClick={() => handleSelectAmount(dashboard.balance)}
                      className="px-2 py-0.5 rounded bg-lime/20 hover:bg-lime/30 text-black font-semibold border border-lime/40"
                      title="Trigger ML fraud detection via full balance drain"
                    >
                      Max (Drain)
                    </button>
                  )}
                </div>
              </div>

              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none font-mono text-sm font-bold text-neutral-600">
                  ₹
                </div>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                  required
                  className="w-full pl-9 pr-4 py-3 rounded-xl bg-neutral-50 border border-neutral-200 text-black font-mono text-lg font-bold focus:outline-none focus:border-black focus:ring-1 focus:ring-black transition-all"
                />
              </div>
              <p className="mt-1 font-mono text-[11px] text-neutral-500">
                Pessimistic concurrency lock verifies sender balance prior to debiting.
              </p>
            </div>

            {/* Security Guarantee Notice */}
            <div className="p-4 rounded-xl bg-neutral-50 border border-neutral-200 space-y-2">
              <div className="flex items-center gap-2 text-xs font-mono font-semibold text-neutral-800">
                <Lock className="w-3.5 h-3.5 text-black" />
                <span>ATOMIC ACID & AI DEFENSE GUARANTEE</span>
              </div>
              <p className="text-xs text-neutral-600 leading-relaxed font-mono">
                Transactions are protected by idempotent keys and evaluated synchronously by Weka Random Forest. If flagged as anomalous, funds are held in escrow for review rather than lost.
              </p>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-4 rounded-xl bg-black text-lime hover:bg-neutral-800 disabled:opacity-50 font-mono text-xs uppercase tracking-wider font-bold transition-all shadow-md flex items-center justify-center gap-2"
            >
              {loading ? (
                <span>EVALUATING FRAUD RISKS & EXECUTING...</span>
              ) : (
                <>
                  <span>CONFIRM & EXECUTE TRANSFER</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        </div>
      </main>

      {/* Result Outcome Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={
          result?.status === 'APPROVED'
            ? 'Transfer Settled & Approved'
            : result?.status === 'PENDING_REVIEW'
            ? 'Flagged for Fraud Review'
            : 'Execution Declined'
        }
        description="Real-time transaction status from SafePay Core"
      >
        {result && (
          <div className="space-y-4 font-mono text-xs">
            {/* Status Header Banner */}
            <div
              className={`p-3.5 rounded-xl border flex items-center justify-between ${
                result.status === 'APPROVED'
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                  : result.status === 'PENDING_REVIEW'
                  ? 'bg-amber-500/10 border-amber-500/30 text-amber-300'
                  : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
              }`}
            >
              <div className="flex items-center gap-2">
                {result.status === 'APPROVED' && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
                {result.status === 'PENDING_REVIEW' && <AlertTriangle className="w-4 h-4 text-amber-400" />}
                {result.status !== 'APPROVED' && result.status !== 'PENDING_REVIEW' && (
                  <AlertCircle className="w-4 h-4 text-rose-400" />
                )}
                <span className="font-semibold uppercase tracking-wider">
                  {result.status === 'PENDING_REVIEW' ? 'Review Hold Active' : result.status}
                </span>
              </div>
              <StatusBadge status={result.status} />
            </div>

            <div className="space-y-2 text-neutral-300">
              <div className="flex justify-between">
                <span className="text-neutral-400">Transaction ID</span>
                <span className="text-white font-semibold">#{result.transactionId || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Amount Transferred</span>
                <span className="text-white font-semibold">{formatCurrency(result.amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Operation Type</span>
                <span className="text-lime">{result.transactionType}</span>
              </div>
              {result.newBalance !== null && (
                <div className="flex justify-between border-t border-white/10 pt-2">
                  <span className="text-neutral-400">Updated Vault Balance</span>
                  <span className="text-lime font-bold text-sm">
                    {formatCurrency(result.newBalance)}
                  </span>
                </div>
              )}
              {result.riskAssessment && (
                <div className="flex justify-between border-t border-white/10 pt-2">
                  <span className="text-neutral-400">Risk Assessment</span>
                  <span className="text-white font-semibold">{result.riskAssessment}</span>
                </div>
              )}
              {result.fraudProbability !== null && (
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-neutral-400">ML Fraud Probability</span>
                    <span className="text-lime font-semibold">
                      {(result.fraudProbability * 100).toFixed(1)}%
                    </span>
                  </div>
                  <div className="w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
                    <div
                      className={`h-full ${
                        result.fraudProbability > 0.5 ? 'bg-rose-500' : 'bg-lime'
                      }`}
                      style={{ width: `${Math.min(100, result.fraudProbability * 100)}%` }}
                    />
                  </div>
                </div>
              )}
              {result.detectionReason && (
                <div className="p-3 rounded-lg bg-neutral-900 border border-white/10 text-neutral-300 mt-2">
                  <span className="text-neutral-400 block mb-0.5 text-[11px] uppercase">Detection Rationale:</span>
                  <span>{result.detectionReason}</span>
                </div>
              )}
            </div>

            <p className="text-neutral-400 leading-relaxed text-[11px] pt-1">
              {result.message}
            </p>

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => {
                  setIsModalOpen(false);
                  router.push('/dashboard');
                }}
                className="flex-1 py-2.5 rounded-xl bg-lime text-black font-semibold text-xs tracking-wider uppercase hover:bg-lime-hover transition-colors shadow-sm"
              >
                Go to Dashboard
              </button>
              <button
                onClick={() => setIsModalOpen(false)}
                className="px-4 py-2.5 rounded-xl bg-neutral-800 text-white font-medium text-xs hover:bg-neutral-700 transition-colors"
              >
                New Transfer
              </button>
            </div>
          </div>
        )}
      </Modal>

      <footer className="py-6 border-t border-neutral-200 bg-white text-center font-mono text-xs text-neutral-500">
        SafePay Banking Services · Protected by Real-Time Machine Learning & ACID Isolation
      </footer>
    </div>
  );
}
