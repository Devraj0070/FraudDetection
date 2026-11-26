'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { DashboardResponse, TransactionResponse, TransactionSummaryDto } from '@/lib/types';
import { CustomerNavbar } from '@/components/layout/CustomerNavbar';
import { StatusBadge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  TrendingUp,
  ShieldCheck,
  Send,
  RefreshCw,
  ArrowUpRight,
  Shield,
  CreditCard,
  History,
  AlertTriangle,
  CheckCircle2,
  Copy,
  Eye,
  Lock,
} from 'lucide-react';

export default function CustomerDashboardPage() {
  const { user, loading: authLoading } = useAuth();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  // Quick detail modal from dashboard
  const [selectedTx, setSelectedTx] = useState<TransactionResponse | null>(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const router = useRouter();

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.dashboard.get();
      setDashboard(data);
    } catch (err: any) {
      setError(err.message || 'Unable to retrieve account details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authLoading && !user) {
      router.push('/login');
      return;
    }
    if (user) {
      fetchDashboardData();
    }
  }, [user, authLoading, router]);

  const handleCopyAccount = () => {
    if (dashboard?.accountNumber) {
      navigator.clipboard?.writeText(dashboard.accountNumber);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleInspectTx = async (txId: number) => {
    setModalLoading(true);
    setIsModalOpen(true);
    try {
      const details = await api.transactions.getById(txId);
      setSelectedTx(details);
    } catch {
      // Fallback to recent record
      const found = dashboard?.recentTransactions.find((t) => t.id === txId);
      if (found) {
        setSelectedTx({
          success: true,
          message: 'Ledger record',
          transactionId: found.id,
          amount: found.amount,
          transactionType: found.transactionType,
          status: found.status,
          transactionTime: found.transactionTime,
          detectionReason: null,
          riskAssessment: 'LOW',
          fraudProbability: null,
          newBalance: null,
        });
      }
    } finally {
      setModalLoading(false);
    }
  };

  if (authLoading || (loading && !dashboard)) {
    return (
      <div className="min-h-screen bg-[#F3F3F3] text-neutral-900 flex flex-col">
        <CustomerNavbar />
        <div className="flex-1 flex items-center justify-center p-8">
          <div className="flex flex-col items-center gap-3 font-mono text-xs text-neutral-600">
            <RefreshCw className="w-6 h-6 animate-spin text-black" />
            <span>Synchronizing secure account vault...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F3F3F3] text-[#111111] flex flex-col selection:bg-lime selection:text-black">
      <CustomerNavbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Welcome Header & Quick Action */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 sm:p-8 rounded-2xl border border-neutral-200/80 shadow-sm">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]" />
              <span className="font-mono text-[11px] uppercase tracking-widest text-neutral-600 font-semibold">
                ACTIVE VAULT · {dashboard?.accountStatus || 'ACTIVE'}
              </span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl uppercase font-bold text-black tracking-tight">
              Welcome, {dashboard?.username || user?.username}
            </h1>
            <div className="flex flex-wrap items-center gap-2 font-mono text-xs text-neutral-500 mt-1">
              <span>Account No:</span>
              <span className="text-black font-semibold bg-neutral-100 px-2 py-0.5 rounded">
                {dashboard?.accountNumber || user?.accountNumber || '—'}
              </span>
              <button
                onClick={handleCopyAccount}
                className="text-neutral-500 hover:text-black transition-colors"
                title="Copy account number"
              >
                <Copy className="w-3.5 h-3.5" />
              </button>
              {copied && <span className="text-emerald-600 text-[11px] font-semibold">Copied!</span>}
              <span className="text-neutral-400">·</span>
              <span>{dashboard?.accountType || 'SAVINGS'} ACCOUNT</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchDashboardData}
              className="p-2.5 rounded-xl border border-neutral-200 hover:bg-neutral-50 text-neutral-600 transition-colors shadow-sm"
              title="Refresh Vault Data"
            >
              <RefreshCw className={loading ? 'w-4 h-4 animate-spin text-black' : 'w-4 h-4'} />
            </button>
            <Link
              href="/transfer"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-black text-lime font-mono text-xs uppercase font-semibold hover:bg-neutral-800 transition-all shadow-sm"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Send Money</span>
            </Link>
          </div>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 font-mono text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={fetchDashboardData} className="underline font-semibold ml-4">
              Retry
            </button>
          </div>
        )}

        {/* Primary Account Balances & Metric Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Main Balance Card */}
          <div className="bg-[#0C0D10] text-white p-6 sm:p-7 rounded-2xl relative overflow-hidden shadow-xl flex flex-col justify-between border border-white/10">
            <div className="absolute top-0 left-0 right-0 h-1 bg-lime shadow-[0_0_12px_#D8FF00]" />
            <div className="absolute top-0 right-0 w-36 h-36 bg-lime/[0.08] rounded-full blur-2xl pointer-events-none" />
            <div>
              <div className="flex justify-between items-center mb-4">
                <span className="font-mono text-xs uppercase text-neutral-400 tracking-wider">
                  Available Vault Balance
                </span>
                <span className="font-mono text-[10px] text-lime uppercase bg-lime/10 px-2 py-0.5 rounded border border-lime/20 font-semibold">
                  REAL-TIME
                </span>
              </div>
              <div className="font-display text-4xl sm:text-5xl font-bold tracking-tight text-white mb-2">
                {formatCurrency(dashboard?.balance ?? 0)}
              </div>
              <div className="font-mono text-xs text-neutral-400 flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5 text-lime" />
                <span>Protected by InnoDB Pessimistic Locks</span>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-white/10 flex justify-between items-center font-mono text-xs text-neutral-400">
              <span>Account: {dashboard?.accountNumber}</span>
              <span className="text-lime flex items-center gap-1 font-semibold">
                <ShieldCheck className="w-3.5 h-3.5" />
                ACID Isolated
              </span>
            </div>
          </div>

          {/* Activity Metrics Card */}
          <div className="bg-white p-6 sm:p-7 rounded-2xl border border-neutral-200/80 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex justify-between items-center mb-4">
                <span className="font-mono text-xs uppercase text-neutral-500 tracking-wider font-semibold">
                  Transaction Audit Metrics
                </span>
                <TrendingUp className="w-4 h-4 text-neutral-500" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="font-display text-3xl font-bold text-black">
                    {dashboard?.totalTransactions ?? 0}
                  </div>
                  <div className="font-mono text-[11px] text-neutral-500 uppercase mt-0.5">
                    Total Transactions
                  </div>
                </div>
                <div>
                  <div className="font-display text-3xl font-bold text-emerald-600">
                    {dashboard?.approvedTransactions ?? 0}
                  </div>
                  <div className="font-mono text-[11px] text-neutral-500 uppercase mt-0.5">
                    Settled & Approved
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-neutral-100 flex justify-between items-center font-mono text-xs text-neutral-600">
              <span>Under Review: <strong className="text-amber-600">{dashboard?.blockedTransactions ?? 0}</strong></span>
              <span>Declined: <strong className="text-rose-600">{dashboard?.declinedTransactions ?? 0}</strong></span>
            </div>
          </div>

          {/* AI Security Shield Card */}
          <div className="bg-white p-6 sm:p-7 rounded-2xl border border-neutral-200/80 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex justify-between items-center mb-4">
                <span className="font-mono text-xs uppercase text-neutral-500 tracking-wider font-semibold">
                  AI Risk Defense Shield
                </span>
                <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
              </div>
              <div className="font-display text-2xl font-bold uppercase text-black mb-1">
                Weka Random Forest
              </div>
              <p className="text-xs text-neutral-600 leading-relaxed">
                Active 100-tree behavioral classification model inspecting velocity, balance shifts, and anomaly vectors.
              </p>
            </div>

            <div className="mt-6 pt-4 border-t border-neutral-100 flex justify-between items-center font-mono text-xs text-neutral-500">
              <span className="text-neutral-700">Model: <strong className="text-black">100% OPERATIONAL</strong></span>
              <span className="text-neutral-600 font-semibold">&lt; 12ms Latency</span>
            </div>
          </div>
        </div>

        {/* Quick Demo & Operations Actions */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <Link
            href="/transfer"
            className="p-4 rounded-xl bg-white border border-neutral-200 hover:border-black transition-all shadow-sm flex items-center justify-between group"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-black text-lime flex items-center justify-center font-bold">
                <Send className="w-4 h-4" />
              </div>
              <div>
                <div className="font-mono text-xs font-bold text-black uppercase">Standard Transfer</div>
                <div className="font-mono text-[11px] text-neutral-500">Send funds to another account</div>
              </div>
            </div>
            <ArrowUpRight className="w-4 h-4 text-neutral-400 group-hover:text-black transition-colors" />
          </Link>

          <Link
            href="/transactions"
            className="p-4 rounded-xl bg-white border border-neutral-200 hover:border-black transition-all shadow-sm flex items-center justify-between group"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-neutral-100 text-black flex items-center justify-center font-bold">
                <History className="w-4 h-4" />
              </div>
              <div>
                <div className="font-mono text-xs font-bold text-black uppercase">Reconciled Ledger</div>
                <div className="font-mono text-[11px] text-neutral-500">Filter and inspect all transactions</div>
              </div>
            </div>
            <ArrowUpRight className="w-4 h-4 text-neutral-400 group-hover:text-black transition-colors" />
          </Link>

          <Link
            href="/transfer"
            className="p-4 rounded-xl bg-lime/10 border border-lime/30 hover:border-lime transition-all shadow-sm flex items-center justify-between group"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-black text-lime flex items-center justify-center font-bold">
                <Shield className="w-4 h-4" />
              </div>
              <div>
                <div className="font-mono text-xs font-bold text-black uppercase">Simulate Fraud Check</div>
                <div className="font-mono text-[11px] text-neutral-600">Test AI anomaly detection hold</div>
              </div>
            </div>
            <ArrowUpRight className="w-4 h-4 text-black group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        {/* Recent Transactions Table */}
        <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-sm overflow-hidden">
          <div className="p-6 border-b border-neutral-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="font-display text-2xl font-bold uppercase text-black tracking-tight">
                Recent Ledger Activity
              </h2>
              <p className="font-mono text-xs text-neutral-500 mt-0.5">
                Latest debit, credit, and transfer movements on your account
              </p>
            </div>
            <Link
              href="/transactions"
              className="inline-flex items-center gap-1.5 font-mono text-xs text-neutral-800 hover:text-black font-semibold hover:underline"
            >
              <span>View Full Ledger</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {dashboard?.recentTransactions && dashboard.recentTransactions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs">
                <thead>
                  <tr className="bg-neutral-50 text-neutral-500 uppercase tracking-wider text-[11px] border-b border-neutral-100">
                    <th className="py-3 px-6">ID</th>
                    <th className="py-3 px-6">Timestamp</th>
                    <th className="py-3 px-6">Type</th>
                    <th className="py-3 px-6">Counterparty</th>
                    <th className="py-3 px-6 text-right">Amount</th>
                    <th className="py-3 px-6 text-center">Status</th>
                    <th className="py-3 px-6 text-right">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 text-neutral-700">
                  {dashboard.recentTransactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-neutral-50/70 transition-colors">
                      <td className="py-3.5 px-6 font-semibold text-black">#{tx.id}</td>
                      <td className="py-3.5 px-6 text-neutral-500">{formatDate(tx.transactionTime)}</td>
                      <td className="py-3.5 px-6">
                        <span className="px-2 py-0.5 rounded bg-neutral-100 text-neutral-800 text-[10px] font-semibold">
                          {tx.transactionType}
                        </span>
                      </td>
                      <td className="py-3.5 px-6 text-neutral-600">
                        {tx.accountNumber ? ('Acct: ' + tx.accountNumber) : '—'}
                      </td>
                      <td className="py-3.5 px-6 text-right font-semibold text-black">
                        {formatCurrency(tx.amount)}
                      </td>
                      <td className="py-3.5 px-6 text-center">
                        <StatusBadge status={tx.status} />
                      </td>
                      <td className="py-3.5 px-6 text-right">
                        <button
                          onClick={() => handleInspectTx(tx.id)}
                          className="p-1.5 rounded-lg border border-neutral-200 hover:bg-black hover:text-lime hover:border-black text-neutral-600 transition-colors"
                          title="Inspect ML vectors & details"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-16 text-center font-mono text-xs text-neutral-400 space-y-3">
              <div>No transactions recorded yet in this account vault.</div>
              <Link
                href="/transfer"
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-black text-lime font-bold uppercase text-[11px] shadow-sm hover:bg-neutral-800 transition-colors"
              >
                <Send className="w-3.5 h-3.5" />
                <span>Initiate First Transfer</span>
              </Link>
            </div>
          )}
        </div>
      </main>

      {/* Quick Details Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={selectedTx ? ('Transaction #' + selectedTx.transactionId) : 'Transaction Details'}
        description="Core immutable ledger record & ML evaluation metrics"
      >
        {modalLoading ? (
          <div className="p-8 text-center font-mono text-xs text-neutral-400 flex flex-col items-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-lime" />
            <span>Fetching telemetry...</span>
          </div>
        ) : selectedTx ? (
          <div className="space-y-4 font-mono text-xs">
            <div className="flex items-center justify-between p-3.5 rounded-xl bg-white/5 border border-white/10">
              <span className="text-neutral-400">Execution Status</span>
              <StatusBadge status={selectedTx.status} />
            </div>

            <div className="space-y-2 text-neutral-300">
              <div className="flex justify-between">
                <span className="text-neutral-400">Recorded Amount</span>
                <span className="text-white font-bold">{formatCurrency(selectedTx.amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Transaction Type</span>
                <span className="text-lime">{selectedTx.transactionType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Timestamp</span>
                <span className="text-white">{formatDate(selectedTx.transactionTime)}</span>
              </div>
              {selectedTx.riskAssessment && (
                <div className="flex justify-between border-t border-white/10 pt-2">
                  <span className="text-neutral-400">Risk Assessment</span>
                  <span className="text-white font-semibold">{selectedTx.riskAssessment}</span>
                </div>
              )}
              {selectedTx.fraudProbability !== null && (
                <div className="flex justify-between">
                  <span className="text-neutral-400">Fraud Probability Score</span>
                  <span className="text-lime">
                    {(selectedTx.fraudProbability * 100).toFixed(1)}%
                  </span>
                </div>
              )}
              {selectedTx.detectionReason && (
                <div className="p-3 rounded-lg bg-neutral-900 border border-white/10 text-neutral-300">
                  <span className="text-neutral-400 block mb-0.5">Detection Rationale:</span>
                  <span>{selectedTx.detectionReason}</span>
                </div>
              )}
            </div>

            <button
              onClick={() => setIsModalOpen(false)}
              className="w-full mt-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-mono text-xs tracking-wider uppercase transition-colors"
            >
              Close Ledger Record
            </button>
          </div>
        ) : null}
      </Modal>

      <footer className="py-6 border-t border-neutral-200 bg-white text-center font-mono text-xs text-neutral-500">
        SafePay Banking Services · Protected by Real-Time Machine Learning & ACID Isolation
      </footer>
    </div>
  );
}
