'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { Navbar } from '@/components/layout/Navbar';
import { Lock, User, Eye, EyeOff, ArrowRight, AlertCircle, ShieldCheck, Sparkles } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const router = useRouter();

  const handleFillDemo = (userVal: string, passVal: string) => {
    setUsername(userVal);
    setPassword(passVal);
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!username.trim() || !password) {
      setError('Please provide both username and password.');
      return;
    }

    setLoading(true);
    try {
      const res = await login({ username: username.trim(), password });
      if (res.success) {
        if (res.role?.toUpperCase().includes('ADMIN')) {
          router.push('/admin');
        } else {
          router.push('/dashboard');
        }
      } else {
        setError(res.message || 'Authentication failed. Please verify credentials.');
      }
    } catch (err: any) {
      setError(err.message || 'Login failed. Please verify your credentials or server status.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#080808] text-white flex flex-col justify-between selection:bg-lime selection:text-black">
      <Navbar />

      <main className="flex-1 flex items-center justify-center px-4 py-24 sm:px-6 lg:px-8 relative overflow-hidden">
        {/* Subtle ambient lighting */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[400px] bg-lime/[0.04] rounded-full blur-[130px] pointer-events-none" />

        <div className="w-full max-w-md relative z-10">
          {/* Card Container */}
          <div className="rounded-2xl bg-[#121316] border border-white/10 p-8 shadow-2xl">
            {/* Header */}
            <div className="text-center mb-6">
              <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-lime/10 border border-lime/20 text-lime mb-4 shadow-[0_0_15px_rgba(216,255,0,0.2)]">
                <Lock className="w-6 h-6" />
              </div>
              <h1 className="font-display text-3xl font-bold tracking-tight text-white uppercase">
                SAFEPAY ACCESS
              </h1>
              <p className="font-mono text-xs text-neutral-400 mt-1 uppercase tracking-wider">
                SECURE AUTHENTICATION GATEWAY
              </p>
            </div>

            {/* Quick Demo Credentials (One-Click Sign In) */}
            <div className="mb-6 p-3 rounded-xl bg-white/[0.03] border border-white/10 font-mono text-xs">
              <div className="flex items-center gap-1.5 text-neutral-400 mb-2 text-[11px] uppercase tracking-wider">
                <Sparkles className="w-3.5 h-3.5 text-lime" />
                <span>Quick Demo Accounts:</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleFillDemo('user', 'user123')}
                  className="px-2.5 py-1.5 rounded-lg bg-neutral-900 hover:bg-neutral-800 border border-white/10 text-neutral-200 text-left hover:border-lime/40 transition-colors flex flex-col"
                >
                  <span className="font-semibold text-white">Demo Customer</span>
                  <span className="text-[10px] text-neutral-500">user / user123</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleFillDemo('admin', 'admin123')}
                  className="px-2.5 py-1.5 rounded-lg bg-neutral-900 hover:bg-neutral-800 border border-white/10 text-neutral-200 text-left hover:border-lime/40 transition-colors flex flex-col"
                >
                  <span className="font-semibold text-lime">Administrator</span>
                  <span className="text-[10px] text-neutral-500">admin / admin123</span>
                </button>
              </div>
            </div>

            {/* Error Banner */}
            {error && (
              <div className="mb-6 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-mono flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                <span>{error}</span>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block font-mono text-xs uppercase tracking-wider text-neutral-400 mb-1.5">
                  Username
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-neutral-500">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    placeholder="e.g. user"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-black/60 border border-white/10 text-white font-mono text-xs focus:outline-none focus:border-lime focus:ring-1 focus:ring-lime transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block font-mono text-xs uppercase tracking-wider text-neutral-400 mb-1.5">
                  Password
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-neutral-500">
                    <Lock className="w-4 h-4" />
                  </div>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    placeholder="••••••••"
                    className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-black/60 border border-white/10 text-white font-mono text-xs focus:outline-none focus:border-lime focus:ring-1 focus:ring-lime transition-all"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-neutral-500 hover:text-white"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-2 py-3 rounded-xl bg-lime hover:bg-lime-hover disabled:opacity-50 text-black font-semibold text-xs tracking-wider uppercase transition-all shadow-[0_0_20px_rgba(216,255,0,0.3)] hover:shadow-[0_0_30px_rgba(216,255,0,0.5)] flex items-center justify-center gap-2"
              >
                {loading ? (
                  <span className="font-mono">AUTHENTICATING...</span>
                ) : (
                  <>
                    <span>SIGN IN SECURELY</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

            {/* Bottom link */}
            <div className="mt-8 pt-6 border-t border-white/10 text-center font-mono text-xs text-neutral-400">
              New to SafePay?{' '}
              <Link href="/register" className="text-lime hover:underline font-semibold">
                Open an Account
              </Link>
            </div>
          </div>

          {/* Quick Info Box */}
          <div className="mt-6 p-4 rounded-xl bg-black/40 border border-white/5 text-center font-mono text-[11px] text-neutral-500">
            <span className="text-lime">EMV & CSRF SECURED</span> · Session cookies strictly partitioned
          </div>
        </div>
      </main>

      <footer className="py-6 border-t border-white/[0.06] text-center font-mono text-xs text-neutral-600">
        SafePay Financial Technologies · Port 8081 Core API Gateway
      </footer>
    </div>
  );
}
