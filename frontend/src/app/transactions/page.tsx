'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { TransactionResponse, TransactionSummaryDto } from '@/lib/types';
import { CustomerNavbar } from '@/components/layout/CustomerNavbar';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/Badge';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  History,
  Search,
  Filter,
  RefreshCw,
  Eye,
  ArrowUpRight,
  ShieldAlert,
  Send,
  X,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

export default function TransactionsPage() {
  const { user, loading: authLoading } = useAuth();
  const [transactions, setTransactions] = useState<TransactionSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  // Transaction details modal
  const [selectedTx, setSelectedTx] = useState<TransactionResponse | null>(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const router = useRouter();

  const fetchHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.transactions.getHistory();
      setTransactions(data || []);
    } catch (err: any) {
      setError(err.message || 'Failed to retrieve transaction ledger.');
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
      fetchHistory();
    }
  }, [user, authLoading, router]);

  const handleInspect = async (id: number) => {
    setModalLoading(true);
    setIsModalOpen(true);
    try {
      const details = await api.transactions.getById(id);
      setSelectedTx(details);
    } catch (err: any) {
      console.warn('Failed to fetch details:', err);
      // Fallback to basic summary if single get endpoint has different schema
      const found = transactions.find((t) => t.id === id);
      if (found) {
        setSelectedTx({
          success: true,
          message: 'Summary record',
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

  // Filtered transactions
  const filtered = useMemo(() => {
    return transactions.filter((t) => {
      const matchesSearch =
        searchTerm === '' ||
        t.id.toString().includes(searchTerm) ||
        (t.accountNumber && t.accountNumber.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesStatus =
        statusFilter === 'ALL' || t.status.toUpperCase() === statusFilter.toUpperCase();

      const matchesType =
        typeFilter === 'ALL' || t.transactionType.toUpperCase() === typeFilter.toUpperCase();

      return matchesSearch && matchesStatus && matchesType;
    });
  }, [transactions, searchTerm, statusFilter, typeFilter]);

  // Summary statistics
  const totalVolume = useMemo(
    () => transactions.reduce((acc, t) => acc + (Number(t.amount) || 0), 0),
    [transactions]
  );
  const approvedCount = useMemo(
    () => transactions.filter((t) => t.status === 'APPROVED').length,
    [transactions]
  );
  const pendingCount = useMemo(
    () => transactions.filter((t) => t.status === 'PENDING_REVIEW').length,
    [transactions]
  );

  const resetFilters = () => {
    setSearchTerm('');
    setStatusFilter('ALL');
    setTypeFilter('ALL');
  };

  return (
    <div className="min-h-screen bg-[#F3F3F3] text-[#111111] flex flex-col selection:bg-lime selection:text-black">
      <CustomerNavbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="w-2 h-2 rounded-full bg-black" />
              <span className="font-mono text-xs uppercase tracking-widest text-neutral-600 font-semibold">
                AUDIT LEDGER & RECONCILIATION
              </span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl uppercase font-bold text-black tracking-tight">
              Transaction History
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchHistory}
              className="p-2.5 rounded-xl border border-neutral-200 bg-white hover:bg-neutral-50 text-neutral-700 transition-colors shadow-sm"
              title="Refresh ledger"
            >
              <RefreshCw className={loading ? 'w-4 h-4 animate-spin text-black' : 'w-4 h-4'} />
            </button>
            <Link
              href="/transfer"
              className="px-4 py-2.5 rounded-xl bg-black text-lime font-mono text-xs uppercase font-semibold hover:bg-neutral-800 transition-all shadow-sm"
            >
              New Transfer
            </Link>
          </div>
        </div>

        {/* Quick Ledger Metrics Bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 rounded-xl bg-white border border-neutral-200/80 shadow-sm font-mono text-xs">
            <div className="text-neutral-500 text-[11px] uppercase mb-1">Total Lifetime Volume</div>
            <div className="font-display text-2xl font-bold text-black">{formatCurrency(totalVolume)}</div>
          </div>
          <div className="p-4 rounded-xl bg-white border border-neutral-200/80 shadow-sm font-mono text-xs">
            <div className="text-neutral-500 text-[11px] uppercase mb-1">Total Entries</div>
            <div className="font-display text-2xl font-bold text-black">{transactions.length}</div>
          </div>
          <div className="p-4 rounded-xl bg-white border border-neutral-200/80 shadow-sm font-mono text-xs">
            <div className="text-neutral-500 text-[11px] uppercase mb-1">Settled / Approved</div>
            <div className="font-display text-2xl font-bold text-emerald-600">{approvedCount}</div>
          </div>
          <div className="p-4 rounded-xl bg-white border border-neutral-200/80 shadow-sm font-mono text-xs">
            <div className="text-neutral-500 text-[11px] uppercase mb-1">Under AI Review</div>
            <div className="font-display text-2xl font-bold text-amber-600">{pendingCount}</div>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="bg-white p-4 sm:p-5 rounded-2xl border border-neutral-200/80 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by ID or Account number..."
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-neutral-50 border border-neutral-200 text-black font-mono text-xs focus:outline-none focus:border-black transition-all"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-black"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 font-mono text-xs text-neutral-600">
              <Filter className="w-3.5 h-3.5 text-neutral-500" />
              <span>Status:</span>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="px-3 py-1.5 rounded-lg bg-neutral-50 border border-neutral-200 text-black font-mono text-xs focus:outline-none focus:border-black"
              >
                <option value="ALL">All Statuses</option>
                <option value="APPROVED">Approved</option>
                <option value="PENDING_REVIEW">Pending Review</option>
                <option value="DECLINED">Declined</option>
                <option value="BLOCKED">Blocked</option>
              </select>
            </div>

            <div className="flex items-center gap-2 font-mono text-xs text-neutral-600">
              <span>Type:</span>
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="px-3 py-1.5 rounded-lg bg-neutral-50 border border-neutral-200 text-black font-mono text-xs focus:outline-none focus:border-black"
              >
                <option value="ALL">All Types</option>
                <option value="TRANSFER">Transfer</option>
                <option value="PAYMENT">Payment</option>
                <option value="CASH_OUT">Cash Out</option>
                <option value="CASH_IN">Cash In</option>
                <option value="DEBIT">Debit</option>
              </select>
            </div>

            {(searchTerm || statusFilter !== 'ALL' || typeFilter !== 'ALL') && (
              <button
                onClick={resetFilters}
                className="px-3 py-1.5 rounded-lg bg-neutral-100 hover:bg-neutral-200 text-neutral-700 font-mono text-xs transition-colors"
              >
                Reset
              </button>
            )}
          </div>
        </div>

        {/* Ledger Table Card */}
        <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-sm overflow-hidden">
          {filtered.length > 0 ? (
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
                    <th className="py-3 px-6 text-right">Inspect</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 text-neutral-700">
                  {filtered.map((tx) => (
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
                          onClick={() => handleInspect(tx.id)}
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
              <div>No transactions match the selected filters.</div>
              {(searchTerm || statusFilter !== 'ALL' || typeFilter !== 'ALL') ? (
                <button
                  onClick={resetFilters}
                  className="px-4 py-2 rounded-xl bg-neutral-100 hover:bg-neutral-200 text-neutral-800 font-semibold"
                >
                  Clear Active Filters
                </button>
              ) : (
                <Link
                  href="/transfer"
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-black text-lime font-bold uppercase text-[11px] shadow-sm hover:bg-neutral-800 transition-colors"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Execute a Transfer</span>
                </Link>
              )}
            </div>
          )}
        </div>
      </main>

      {/* Transaction Details Modal */}
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
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-neutral-400">Fraud Probability Score</span>
                    <span className="text-lime font-semibold">
                      {(selectedTx.fraudProbability * 100).toFixed(1)}%
                    </span>
                  </div>
                  <div className="w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
                    <div
                      className={`h-full ${
                        selectedTx.fraudProbability > 0.5 ? 'bg-rose-500' : 'bg-lime'
                      }`}
                      style={{ width: `${Math.min(100, selectedTx.fraudProbability * 100)}%` }}
                    />
                  </div>
                </div>
              )}
              {selectedTx.detectionReason && (
                <div className="p-3 rounded-lg bg-neutral-900 border border-white/10 text-neutral-300">
                  <span className="text-neutral-400 block mb-0.5 text-[11px] uppercase">Detection Rationale:</span>
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
