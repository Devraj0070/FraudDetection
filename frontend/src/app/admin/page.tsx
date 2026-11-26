'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { FraudAlert } from '@/lib/types';
import { StatusBadge, Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  AlertCircle,
  ShieldAlert,
  ShieldCheck,
  Cpu,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  RefreshCw,
  LogOut,
  Terminal,
  Activity,
  Layers,
  Lock,
  Eye,
  Sliders,
  Filter,
} from 'lucide-react';

export default function AdminMonitoringPage() {
  const { user, loading: authLoading, isAdmin, logout } = useAuth();
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [pending, setPending] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [alertFilter, setAlertFilter] = useState<'ALL' | 'OPEN' | 'RESOLVED'>('ALL');

  // Review action modal
  const [activeAction, setActiveAction] = useState<{
    type: 'approve' | 'reject';
    txnId: number;
    amount?: number;
    senderAcct?: string;
    receiverAcct?: string;
    txnType?: string;
    alertId?: number;
  } | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const router = useRouter();

  const fetchAdminData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [alertsData, pendingData] = await Promise.all([
        api.admin.getAlerts().catch(() => []),
        api.admin.getPending().catch(() => []),
      ]);
      setAlerts(alertsData || []);
      setPending(pendingData || []);
    } catch (err: any) {
      setError(err.message || 'Failed to load surveillance records.');
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
      fetchAdminData();
    }
  }, [user, authLoading, router]);

  const handleExecuteAction = async () => {
    if (!activeAction) return;
    setActionLoading(true);
    setError(null);
    setStatusMessage(null);

    try {
      const notes = reviewNotes.trim() || (activeAction.type === 'approve' ? 'Approved by admin' : 'Rejected by admin');
      if (activeAction.type === 'approve') {
        const res = await api.admin.approve(activeAction.txnId, notes);
        setStatusMessage(res.message || 'Transaction #' + activeAction.txnId + ' approved successfully.');
      } else {
        const res = await api.admin.reject(activeAction.txnId, notes);
        setStatusMessage(res.message || 'Transaction #' + activeAction.txnId + ' rejected.');
      }
      setActiveAction(null);
      setReviewNotes('');
      // Refresh admin queue
      await fetchAdminData();
    } catch (err: any) {
      setError(err.message || 'Failed to perform review decision.');
    } finally {
      setActionLoading(false);
    }
  };

  const filteredAlerts = useMemo(() => {
    if (alertFilter === 'ALL') return alerts;
    return alerts.filter((a) => (a.status || 'OPEN').toUpperCase() === alertFilter);
  }, [alerts, alertFilter]);

  return (
    <div className="min-h-screen bg-[#080808] text-white flex flex-col selection:bg-lime selection:text-black font-sans">
      {/* Admin Top Navigation */}
      <header className="bg-[#0C0D10] border-b border-white/10 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/" className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-black border border-white/10 flex items-center justify-center">
                <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
              </div>
              <span className="font-display text-2xl font-bold tracking-wider text-white">
                SAFEPAY
              </span>
              <span className="font-mono text-[10px] tracking-widest text-lime px-2 py-0.5 rounded bg-lime/10 border border-lime/20 uppercase font-semibold">
                SURVEILLANCE RADAR
              </span>
            </Link>
          </div>

          <div className="flex items-center gap-3 sm:gap-4">
            <Link
              href="/dashboard"
              className="text-xs font-mono text-neutral-400 hover:text-white transition-colors hidden sm:inline-block"
            >
              Customer View
            </Link>

            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-neutral-900 border border-white/10 font-mono text-xs">
              <span className="w-2 h-2 rounded-full bg-lime animate-pulse" />
              <span className="text-white font-medium">{user?.username || 'Admin'}</span>
              <span className="text-[10px] text-lime font-semibold px-1 rounded bg-lime/10">
                {user?.role || 'ROLE_ADMIN'}
              </span>
            </div>

            <button
              onClick={logout}
              className="p-2 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-900 transition-colors"
              title="Sign Out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="w-2 h-2 rounded-full bg-lime" />
              <span className="font-mono text-xs uppercase tracking-widest text-neutral-400 font-semibold">
                REAL-TIME FRAUD OPERATIONS CONSOLE
              </span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl uppercase font-bold text-white tracking-tight">
              Fraud Review Command Center
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchAdminData}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-neutral-900 border border-white/10 hover:border-lime text-white font-mono text-xs transition-colors shadow-sm"
            >
              <RefreshCw className={loading ? 'w-3.5 h-3.5 animate-spin text-lime' : 'w-3.5 h-3.5 text-lime'} />
              <span>Poll Live Feeds</span>
            </button>
          </div>
        </div>

        {/* Status & Error Notifications */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 font-mono text-xs flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={() => setError(null)} className="text-neutral-400 hover:text-white">
              <XCircle className="w-4 h-4" />
            </button>
          </div>
        )}

        {statusMessage && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 font-mono text-xs flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>{statusMessage}</span>
            </div>
            <button onClick={() => setStatusMessage(null)} className="text-neutral-400 hover:text-white">
              <XCircle className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* System Health & Operations Ticker */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-5 rounded-2xl bg-[#111215] border border-white/10">
            <div className="flex justify-between items-center text-neutral-400 font-mono text-xs mb-2">
              <span>ACTIVE FRAUD ALERTS</span>
              <ShieldAlert className="w-4 h-4 text-rose-400" />
            </div>
            <div className="font-display text-3xl font-bold text-white">
              {alerts.length}
            </div>
            <div className="font-mono text-[10px] text-neutral-500 mt-1 uppercase">
              Weka ML Anomalies Logged
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-[#111215] border border-white/10">
            <div className="flex justify-between items-center text-neutral-400 font-mono text-xs mb-2">
              <span>PENDING SIGN-OFF</span>
              <AlertTriangle className="w-4 h-4 text-amber-400" />
            </div>
            <div className="font-display text-3xl font-bold text-amber-400">
              {pending.length}
            </div>
            <div className="font-mono text-[10px] text-neutral-500 mt-1 uppercase">
              Requires Admin Decision
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-[#111215] border border-white/10">
            <div className="flex justify-between items-center text-neutral-400 font-mono text-xs mb-2">
              <span>AI CLASSIFIER</span>
              <Cpu className="w-4 h-4 text-lime" />
            </div>
            <div className="font-display text-2xl font-bold text-lime uppercase">
              ONLINE
            </div>
            <div className="font-mono text-[10px] text-neutral-500 mt-1">
              Random Forest · 100 Trees
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-[#111215] border border-white/10">
            <div className="flex justify-between items-center text-neutral-400 font-mono text-xs mb-2">
              <span>LOCKING PROTOCOL</span>
              <Lock className="w-4 h-4 text-neutral-400" />
            </div>
            <div className="font-display text-xl font-bold text-white uppercase">
              PESSIMISTIC_WRITE
            </div>
            <div className="font-mono text-[10px] text-neutral-500 mt-1">
              Deterministic Dual-Account Locks
            </div>
          </div>
        </div>

        {/* Pending Review Queue (Transactions needing approval/rejection) */}
        <div className="rounded-2xl bg-[#111215] border border-white/10 overflow-hidden shadow-2xl">
          <div className="p-6 border-b border-white/10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse" />
                <h2 className="font-display text-2xl font-bold uppercase text-white tracking-wide">
                  Pending Transaction Queue
                </h2>
              </div>
              <p className="font-mono text-xs text-neutral-400 mt-1">
                Transactions suspended on review hold pending human administrative adjudication
              </p>
            </div>
            <span className="font-mono text-xs text-amber-400 px-3 py-1 rounded bg-amber-400/10 border border-amber-400/20 font-semibold">
              {pending.length} Transactions Awaiting Decision
            </span>
          </div>

          {pending.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs">
                <thead>
                  <tr className="bg-neutral-900 text-neutral-400 uppercase tracking-wider text-[11px] border-b border-white/10">
                    <th className="py-3.5 px-6">Txn ID</th>
                    <th className="py-3.5 px-6">Timestamp</th>
                    <th className="py-3.5 px-6">Sender Account</th>
                    <th className="py-3.5 px-6">Type</th>
                    <th className="py-3.5 px-6 text-right">Amount</th>
                    <th className="py-3.5 px-6 text-center">Status</th>
                    <th className="py-3.5 px-6 text-right">Review Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-neutral-300">
                  {pending.map((p) => {
                    const txId = p.id || p.transactionId;
                    const amount = p.amount;
                    const type = p.transactionType || 'TRANSFER';
                    const time = p.transactionTime || p.createdAt;
                    const acct = p.account?.accountNumber || p.accountNumber || '—';
                    const rcvrAcct = p.receiverAccount?.accountNumber || p.receiverAccountNumber || '—';

                    return (
                      <tr key={txId} className="hover:bg-white/[0.02] transition-colors">
                        <td className="py-3.5 px-6 font-semibold text-white">#{txId}</td>
                        <td className="py-3.5 px-6 text-neutral-400">{formatDate(time)}</td>
                        <td className="py-3.5 px-6 text-neutral-300">{acct}</td>
                        <td className="py-3.5 px-6">
                          <span className="px-2 py-0.5 rounded bg-white/5 border border-white/10 text-neutral-300 text-[10px]">
                            {type}
                          </span>
                        </td>
                        <td className="py-3.5 px-6 text-right font-bold text-white">
                          {formatCurrency(amount)}
                        </td>
                        <td className="py-3.5 px-6 text-center">
                          <StatusBadge status="PENDING_REVIEW" />
                        </td>
                        <td className="py-3.5 px-6 text-right">
                          <div className="inline-flex items-center gap-2">
                            <button
                              onClick={() =>
                                setActiveAction({
                                  type: 'approve',
                                  txnId: txId,
                                  amount,
                                  senderAcct: acct,
                                  receiverAcct: rcvrAcct,
                                  txnType: type,
                                })
                              }
                              className="px-3 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 transition-colors font-semibold"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() =>
                                setActiveAction({
                                  type: 'reject',
                                  txnId: txId,
                                  amount,
                                  senderAcct: acct,
                                  receiverAcct: rcvrAcct,
                                  txnType: type,
                                })
                              }
                              className="px-3 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-colors font-semibold"
                            >
                              Reject
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-12 text-center font-mono text-xs text-neutral-500">
              Zero transactions currently awaiting administrative approval. System healthy.
            </div>
          )}
        </div>

        {/* Live Fraud Alerts Feed */}
        <div className="rounded-2xl bg-[#111215] border border-white/10 overflow-hidden shadow-2xl">
          <div className="p-6 border-b border-white/10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 shadow-[0_0_8px_#F43F5E]" />
                <h2 className="font-display text-2xl font-bold uppercase text-white tracking-wide">
                  Fraud Anomaly Ledger
                </h2>
              </div>
              <p className="font-mono text-xs text-neutral-400 mt-1">
                Real-time incident records emitted by Random Forest model and behavioral anomaly rules
              </p>
            </div>

            {/* Alert Filter Tabs */}
            <div className="flex items-center gap-1 bg-black/60 p-1 rounded-xl border border-white/10 font-mono text-xs">
              {(['ALL', 'OPEN', 'RESOLVED'] as const).map((filter) => (
                <button
                  key={filter}
                  onClick={() => setAlertFilter(filter)}
                  className={`px-3 py-1 rounded-lg transition-colors ${
                    alertFilter === filter
                      ? 'bg-white/10 text-white font-semibold'
                      : 'text-neutral-400 hover:text-white'
                  }`}
                >
                  {filter}
                </button>
              ))}
            </div>
          </div>

          {filteredAlerts.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs">
                <thead>
                  <tr className="bg-neutral-900 text-neutral-400 uppercase tracking-wider text-[11px] border-b border-white/10">
                    <th className="py-3.5 px-6">Alert ID</th>
                    <th className="py-3.5 px-6">Detection Time</th>
                    <th className="py-3.5 px-6">Severity</th>
                    <th className="py-3.5 px-6">Anomaly Type</th>
                    <th className="py-3.5 px-6">Txn Ref</th>
                    <th className="py-3.5 px-6">Status</th>
                    <th className="py-3.5 px-6">Resolution</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-neutral-300">
                  {filteredAlerts.map((alert) => (
                    <tr key={alert.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3.5 px-6 font-semibold text-white">ALT-{alert.id}</td>
                      <td className="py-3.5 px-6 text-neutral-400">{formatDate(alert.createdAt)}</td>
                      <td className="py-3.5 px-6">
                        <StatusBadge status={alert.severity} />
                      </td>
                      <td className="py-3.5 px-6 text-white font-medium">
                        {alert.alertType || 'ANOMALOUS_VELOCITY'}
                      </td>
                      <td className="py-3.5 px-6 font-mono text-lime font-semibold">
                        {alert.transaction?.id ? ('#' + alert.transaction.id) : '—'}
                      </td>
                      <td className="py-3.5 px-6">
                        <Badge
                          variant={alert.status === 'RESOLVED' ? 'success' : 'warning'}
                        >
                          {alert.status || 'OPEN'}
                        </Badge>
                      </td>
                      <td className="py-3.5 px-6 text-neutral-400">
                        {alert.decision ? (
                          <span className="text-white font-semibold">
                            {alert.decision}
                            {alert.reviewNotes && (
                              <span className="text-neutral-500 font-normal ml-2">
                                ({alert.reviewNotes})
                              </span>
                            )}
                          </span>
                        ) : (
                          <span className="text-neutral-500 italic">Pending review</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-12 text-center font-mono text-xs text-neutral-500">
              No fraud alerts match the selected filter.
            </div>
          )}
        </div>
      </main>

      {/* Review Decision Confirmation Modal with Forensic Details */}
      <Modal
        isOpen={Boolean(activeAction)}
        onClose={() => setActiveAction(null)}
        title={activeAction?.type === 'approve' ? 'Approve Suspended Transaction' : 'Reject & Block Transaction'}
        description={activeAction ? ('Review decision on Transaction #' + activeAction.txnId) : ''}
      >
        <div className="space-y-4 font-mono text-xs">
          {/* Forensic Context Box */}
          {activeAction && (
            <div className="p-3.5 rounded-xl bg-black/60 border border-white/10 space-y-2 text-neutral-300">
              <div className="flex justify-between">
                <span className="text-neutral-500">Transaction ID:</span>
                <span className="text-white font-bold">#{activeAction.txnId}</span>
              </div>
              {activeAction.amount !== undefined && (
                <div className="flex justify-between">
                  <span className="text-neutral-500">Hold Amount:</span>
                  <span className="text-lime font-bold">{formatCurrency(activeAction.amount)}</span>
                </div>
              )}
              {activeAction.senderAcct && (
                <div className="flex justify-between">
                  <span className="text-neutral-500">Debited Account:</span>
                  <span className="text-white">{activeAction.senderAcct}</span>
                </div>
              )}
              {activeAction.receiverAcct && (
                <div className="flex justify-between">
                  <span className="text-neutral-500">Credited Account:</span>
                  <span className="text-white">{activeAction.receiverAcct}</span>
                </div>
              )}
            </div>
          )}

          <p className="text-neutral-300 leading-relaxed text-[11px]">
            {activeAction?.type === 'approve'
              ? 'Approving this transaction will immediately release the hold, acquire pessimistic row locks, and finalize balance settlement.'
              : 'Rejecting this transaction will mark the record as DECLINED/REJECTED and safely release held funds back to sender available balance.'}
          </p>

          <div>
            <label className="block text-neutral-400 uppercase text-[11px] mb-1.5 font-semibold">
              Reviewer Notes (Optional)
            </label>
            <textarea
              value={reviewNotes}
              onChange={(e) => setReviewNotes(e.target.value)}
              placeholder="e.g. Identity verified via secondary customer phone channel."
              rows={3}
              className="w-full px-3.5 py-2.5 rounded-xl bg-black/60 border border-white/10 text-white font-mono text-xs focus:outline-none focus:border-lime transition-all"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              onClick={handleExecuteAction}
              disabled={actionLoading}
              className={
                activeAction?.type === 'approve'
                  ? 'flex-1 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-black font-bold uppercase tracking-wider text-xs transition-colors shadow-sm'
                  : 'flex-1 py-3 rounded-xl bg-rose-500 hover:bg-rose-600 text-white font-bold uppercase tracking-wider text-xs transition-colors shadow-sm'
              }
            >
              {actionLoading
                ? 'RECORDING DECISION...'
                : activeAction?.type === 'approve'
                ? 'CONFIRM APPROVAL'
                : 'CONFIRM REJECTION'}
            </button>
            <button
              onClick={() => setActiveAction(null)}
              className="px-4 py-3 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs uppercase tracking-wider transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      </Modal>

      <footer className="py-6 border-t border-white/[0.06] text-center font-mono text-xs text-neutral-600 bg-[#0C0D10]">
        SafePay Surveillance Engine · Port 8081 Core API · Java 25 & Weka Random Forest
      </footer>
    </div>
  );
}
