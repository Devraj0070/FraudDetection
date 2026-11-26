'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/layout/Navbar';
import { Footer } from '@/components/layout/Footer';
import { HeroCanvas } from '@/components/3d/HeroCanvas';
import {
  ShieldCheck,
  Lock,
  Zap,
  ArrowRight,
  CheckCircle2,
  AlertTriangle,
  CreditCard,
  Send,
  History,
  Eye,
  Wallet,
  Smartphone,
  Sparkles,
  ChevronRight,
  ArrowUpRight,
  RefreshCw,
} from 'lucide-react';

export default function LandingPage() {
  const [activeTab, setActiveTab] = useState<'customer' | 'transfers' | 'desktop'>('customer');

  const bankingServices = [
    {
      title: 'Digital Smart Accounts',
      desc: 'Seamless account setup with instant balance visibility, segregated available balances, and full transaction control.',
      icon: Wallet,
      tag: 'INSTANT ONBOARDING',
    },
    {
      title: 'Instant Transfers',
      desc: 'Send money to any account with sub-second execution, automated recipient validation, and instant receipt generation.',
      icon: Send,
      tag: 'REAL-TIME SETTLEMENT',
    },
    {
      title: 'Intelligent Fraud Defense',
      desc: 'Continuous pre-authorization screening stops unauthorized withdrawals and velocity spikes before funds leave your account.',
      icon: ShieldCheck,
      tag: 'PRE-AUTH SHIELD',
    },
    {
      title: 'Transparent Ledger History',
      desc: 'Complete audit trail of every credit, debit, and transfer with real-time status tracking, search filters, and exportable receipts.',
      icon: History,
      tag: 'AUDIT LEDGER',
    },
  ];

  const securityPillars = [
    {
      title: 'Zero Fraud Liability',
      desc: 'You are completely protected against unauthorized transactions. Any flagged activity is held in secure escrow while being reviewed.',
      icon: ShieldCheck,
    },
    {
      title: 'Real-Time Anomaly Interception',
      desc: 'Our intelligent security engine evaluates transaction velocity, location deviations, and sudden spikes in milliseconds before debiting.',
      icon: Zap,
    },
    {
      title: 'Escrow Funds Protection',
      desc: 'Disputed or high-risk transfers are safely reserved in escrow, preventing unauthorized balance drain without corrupting your account balance.',
      icon: Lock,
    },
    {
      title: 'Omnichannel Security',
      desc: 'Seamless banking protection synchronized across the web platform and the dedicated native desktop application.',
      icon: Smartphone,
    },
  ];

  return (
    <div className="min-h-screen bg-[#080808] text-white selection:bg-lime selection:text-black">
      <Navbar />

      {/* =========================================================================
          SECTION 1 — HERO
          Full-width dark background, 3D interactive card, transaction network, bold display
      ========================================================================= */}
      <section className="relative min-h-[92vh] flex flex-col justify-center pt-24 pb-16 overflow-hidden">
        {/* Subtle background radial ambient lights */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[500px] bg-lime/[0.04] rounded-full blur-[140px] pointer-events-none" />
        <div className="absolute top-1/3 left-1/4 w-[400px] h-[400px] bg-blue-600/[0.03] rounded-full blur-[120px] pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            {/* Left Column: Editorial Headline & Actions */}
            <div className="lg:col-span-6 z-10">
              {/* Product Badge */}
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/[0.04] border border-white/10 mb-6 font-mono text-xs text-neutral-300">
                <span className="w-2 h-2 rounded-full bg-lime animate-pulse shadow-[0_0_8px_#D8FF00]" />
                <span className="text-lime font-semibold">SAFEPAY BANKING</span>
                <span className="text-neutral-500">·</span>
                <span>INTELLIGENT TRANSACTION DEFENSE</span>
              </div>

              {/* Main Headline */}
              <h1 className="font-display text-5xl sm:text-7xl xl:text-8xl tracking-tight leading-[0.92] text-white uppercase font-bold mb-6">
                YOUR MONEY.
                <br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-lime via-white to-neutral-400">
                  YOUR CONTROL.
                </span>
              </h1>

              {/* Supporting Subhead */}
              <p className="font-mono text-xs sm:text-sm text-neutral-400 uppercase tracking-widest mb-4">
                SMARTER TRANSACTIONS. SAFER PAYMENTS.
              </p>
              <p className="text-sm sm:text-base text-neutral-400 leading-relaxed max-w-xl mb-8">
                Experience next-generation digital banking with instant money transfers, real-time balance tracking, and automated fraud defense. Every payment is protected before authorization.
              </p>

              {/* CTAs */}
              <div className="flex flex-wrap items-center gap-4">
                <Link
                  href="/register"
                  className="inline-flex items-center gap-3 px-7 py-3.5 rounded-xl bg-lime hover:bg-lime-hover text-black font-semibold text-sm tracking-wide transition-all duration-200 shadow-[0_0_25px_rgba(216,255,0,0.35)] hover:shadow-[0_0_35px_rgba(216,255,0,0.55)] group"
                >
                  <span>OPEN ACCOUNT</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </Link>

                <Link
                  href="/login"
                  className="inline-flex items-center gap-2 px-6 py-3.5 rounded-xl bg-neutral-900/90 hover:bg-neutral-800 border border-white/10 text-white font-mono text-xs uppercase tracking-wider transition-colors"
                >
                  <Lock className="w-3.5 h-3.5 text-lime" />
                  <span>SIGN IN</span>
                </Link>

                <a
                  href="#services"
                  className="inline-flex items-center gap-2 px-4 py-3.5 text-neutral-400 hover:text-white font-mono text-xs transition-colors"
                >
                  <span>EXPLORE SERVICES</span>
                  <ChevronRight className="w-3.5 h-3.5 text-lime" />
                </a>
              </div>

              {/* Live Metric Tickers */}
              <div className="grid grid-cols-3 gap-4 mt-12 pt-8 border-t border-white/[0.08]">
                <div>
                  <div className="font-display text-2xl sm:text-3xl font-bold text-white tracking-wide">
                    &lt; 1<span className="text-lime text-base sm:text-lg">s</span>
                  </div>
                  <div className="font-mono text-[10px] text-neutral-500 uppercase tracking-wider mt-0.5">
                    Instant Transfer
                  </div>
                </div>
                <div>
                  <div className="font-display text-2xl sm:text-3xl font-bold text-white tracking-wide">
                    100<span className="text-lime text-base sm:text-lg">%</span>
                  </div>
                  <div className="font-mono text-[10px] text-neutral-500 uppercase tracking-wider mt-0.5">
                    Zero Liability
                  </div>
                </div>
                <div>
                  <div className="font-display text-2xl sm:text-3xl font-bold text-white tracking-wide">
                    24<span className="text-lime text-base sm:text-lg">/7</span>
                  </div>
                  <div className="font-mono text-[10px] text-neutral-500 uppercase tracking-wider mt-0.5">
                    Live Protection
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: 3D Interactive Payment Card & Transaction Network */}
            <div className="lg:col-span-6 flex items-center justify-center relative">
              <div className="w-full relative">
                <HeroCanvas />
                <div className="text-center font-mono text-[11px] text-neutral-500 mt-2">
                  <span className="text-lime">●</span> 3D SAFEPAY SECURE CARD · ROTATE & TILT WITH CURSOR
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 2 — CORE BANKING SERVICES
      ========================================================================= */}
      <section id="services" className="py-24 bg-[#0B0C0E] border-t border-b border-white/[0.08] relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 gap-6">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-lime px-2.5 py-1 rounded bg-lime/10 border border-lime/20 inline-block mb-3">
                INTELLIGENT BANKING
              </span>
              <h2 className="font-display text-4xl sm:text-6xl uppercase font-bold text-white tracking-tight">
                BUILT FOR SPEED. FORTIFIED FOR SAFETY.
              </h2>
            </div>
            <p className="text-neutral-400 max-w-md text-sm leading-relaxed">
              Every financial service you need in one unified platform. Manage balances, initiate direct transfers, and bank with absolute confidence.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {bankingServices.map((service, idx) => {
              const Icon = service.icon;
              return (
                <div
                  key={idx}
                  className="p-8 rounded-2xl bg-[#121316] border border-white/[0.08] hover:border-lime/40 transition-all duration-300 group relative flex flex-col justify-between"
                >
                  <div>
                    <div className="flex justify-between items-start mb-6">
                      <div className="w-12 h-12 rounded-xl bg-black border border-white/10 flex items-center justify-center text-lime group-hover:scale-110 transition-transform shadow-[0_0_15px_rgba(216,255,0,0.15)]">
                        <Icon className="w-6 h-6" />
                      </div>
                      <span className="font-mono text-[10px] text-neutral-500 uppercase tracking-widest">
                        {service.tag}
                      </span>
                    </div>

                    <h3 className="font-display text-2xl font-bold uppercase text-white mb-3">
                      {service.title}
                    </h3>

                    <p className="text-neutral-400 text-xs sm:text-sm leading-relaxed">
                      {service.desc}
                    </p>
                  </div>

                  <div className="pt-6 mt-6 border-t border-white/[0.06] flex items-center gap-2 text-lime font-mono text-xs font-semibold group-hover:translate-x-1 transition-transform">
                    <span>LEARN MORE</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 3 — ENTERPRISE SECURITY & CUSTOMER TRUST
      ========================================================================= */}
      <section id="security" className="py-24 bg-[#080808] relative overflow-hidden">
        <div className="absolute top-1/2 right-1/4 w-[500px] h-[500px] bg-lime/[0.03] rounded-full blur-[140px] pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            {/* Left Column: Heading and Value Prop */}
            <div className="lg:col-span-5">
              <span className="font-mono text-xs uppercase tracking-widest text-lime px-2.5 py-1 rounded bg-lime/10 border border-lime/20 inline-block mb-3">
                PEACE OF MIND
              </span>
              <h2 className="font-display text-4xl sm:text-6xl uppercase font-bold text-white tracking-tight mb-6">
                PROTECTION THAT NEVER SLEEPS.
              </h2>
              <p className="text-neutral-400 text-sm leading-relaxed mb-8">
                Traditional banks notify you after unauthorized funds have already left your account. SafePay evaluates every transaction before authorization, intercepting fraud in real time.
              </p>

              <div className="p-6 rounded-2xl bg-[#111215] border border-white/10 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-lime/10 border border-lime/20 flex items-center justify-center text-lime">
                    <CheckCircle2 className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="font-display text-sm font-bold uppercase text-white">Instant Escrow Quarantine</h4>
                    <p className="text-xs text-neutral-400">Suspicious activities never deduct from your balance without your confirmation.</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-lime/10 border border-lime/20 flex items-center justify-center text-lime">
                    <CheckCircle2 className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="font-display text-sm font-bold uppercase text-white">Non-Destructive Accounting</h4>
                    <p className="text-xs text-neutral-400">Available balance and ledger balance remain segregated for full financial safety.</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: 4 Pillars Grid */}
            <div className="lg:col-span-7 grid grid-cols-1 sm:grid-cols-2 gap-6">
              {securityPillars.map((pillar, idx) => {
                const Icon = pillar.icon;
                return (
                  <div
                    key={idx}
                    className="p-6 rounded-2xl bg-[#121316] border border-white/[0.08] hover:border-lime/30 transition-colors"
                  >
                    <div className="w-10 h-10 rounded-lg bg-black border border-white/10 flex items-center justify-center text-lime mb-4">
                      <Icon className="w-5 h-5" />
                    </div>
                    <h3 className="font-display text-xl font-bold uppercase text-white mb-2">
                      {pillar.title}
                    </h3>
                    <p className="text-xs sm:text-sm text-neutral-400 leading-relaxed">
                      {pillar.desc}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 4 — INTERACTIVE EXPERIENCE SHOWCASE
      ========================================================================= */}
      <section id="experience" className="py-24 bg-[#0E0F12] border-t border-b border-white/[0.08]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto mb-16">
            <span className="font-mono text-xs uppercase tracking-widest text-lime px-2.5 py-1 rounded bg-lime/10 border border-lime/20 inline-block mb-3">
              PREMIUM EXPERIENCE
            </span>
            <h2 className="font-display text-4xl sm:text-6xl uppercase font-bold text-white tracking-tight mb-4">
              INTUITIVE INTERFACES FOR WEB & DESKTOP.
            </h2>
            <p className="text-neutral-400 text-sm leading-relaxed">
              Designed from the ground up for modern financial management. Seamlessly transition between web banking and our dedicated high-performance desktop client.
            </p>

            {/* Tab Selector */}
            <div className="inline-flex p-1.5 rounded-xl bg-black border border-white/10 mt-8 font-mono text-xs uppercase">
              <button
                onClick={() => setActiveTab('customer')}
                className={`px-5 py-2.5 rounded-lg transition-all ${
                  activeTab === 'customer'
                    ? 'bg-lime text-black font-semibold shadow-[0_0_15px_rgba(216,255,0,0.3)]'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Customer Dashboard
              </button>
              <button
                onClick={() => setActiveTab('transfers')}
                className={`px-5 py-2.5 rounded-lg transition-all ${
                  activeTab === 'transfers'
                    ? 'bg-lime text-black font-semibold shadow-[0_0_15px_rgba(216,255,0,0.3)]'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Instant Transfers
              </button>
              <button
                onClick={() => setActiveTab('desktop')}
                className={`px-5 py-2.5 rounded-lg transition-all ${
                  activeTab === 'desktop'
                    ? 'bg-lime text-black font-semibold shadow-[0_0_15px_rgba(216,255,0,0.3)]'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Desktop Application
              </button>
            </div>
          </div>

          {/* Interactive Screen Mockup Card */}
          <div className="rounded-2xl bg-[#141519] border border-white/10 p-6 sm:p-10 shadow-2xl">
            {activeTab === 'customer' && (
              <div className="space-y-8">
                {/* Simulated Customer Dashboard Header */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-6 border-b border-white/10">
                  <div>
                    <span className="font-mono text-xs text-lime uppercase tracking-widest">
                      SAVINGS ACCOUNT · ACC-100000000002
                    </span>
                    <h3 className="font-display text-3xl sm:text-4xl font-bold uppercase text-white mt-1">
                      ₹100,000.00
                    </h3>
                    <div className="flex items-center gap-2 mt-1 font-mono text-xs text-neutral-400">
                      <span>Available Balance: <span className="text-white font-semibold">₹100,000.00</span></span>
                      <span>·</span>
                      <span className="text-emerald-400 inline-flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Active & Protected
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <Link
                      href="/transfer"
                      className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-lime hover:bg-lime-hover text-black font-semibold text-xs tracking-wide transition-all shadow-[0_0_15px_rgba(216,255,0,0.3)]"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>SEND MONEY</span>
                    </Link>
                    <Link
                      href="/dashboard"
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-neutral-900 border border-white/10 text-white font-mono text-xs hover:border-white/30 transition-colors"
                    >
                      <span>VIEW FULL DASHBOARD</span>
                      <ArrowUpRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>

                {/* Simulated Activity Stream */}
                <div>
                  <h4 className="font-mono text-xs uppercase tracking-wider text-neutral-400 mb-4">
                    Recent Verified Transactions
                  </h4>
                  <div className="space-y-3 font-mono text-xs">
                    <div className="p-4 rounded-xl bg-black/60 border border-white/5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                          <CheckCircle2 className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="text-white font-semibold">Transfer to ACC-100000000001</div>
                          <div className="text-[11px] text-neutral-500">PAYMENT · Settled Instantly</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-emerald-400 font-semibold">- ₹2,500.00</div>
                        <div className="text-[10px] text-neutral-500">VERIFIED & APPROVED</div>
                      </div>
                    </div>

                    <div className="p-4 rounded-xl bg-black/60 border border-white/5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                          <CheckCircle2 className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="text-white font-semibold">Direct Deposit</div>
                          <div className="text-[11px] text-neutral-500">CASH_IN · Account Credit</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-lime font-semibold">+ ₹50,000.00</div>
                        <div className="text-[10px] text-neutral-500">SETTLED IN LEDGER</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'transfers' && (
              <div className="space-y-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-6 border-b border-white/10">
                  <div>
                    <span className="font-mono text-xs text-lime uppercase tracking-widest">
                      SEAMLESS DISPATCH
                    </span>
                    <h3 className="font-display text-2xl font-bold uppercase text-white mt-1">
                      Direct Peer-to-Peer Payments
                    </h3>
                  </div>
                  <div className="font-mono text-xs text-neutral-400">
                    Protected by Real-Time Fraud Shield
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 font-mono text-xs">
                  <div className="p-5 rounded-xl bg-black/60 border border-white/5 space-y-2">
                    <span className="text-neutral-500">01. Choose Recipient</span>
                    <div className="text-white font-semibold">Account Number or ID</div>
                    <p className="text-neutral-400 text-[11px]">Instant automated validation ensures funds only reach verified active accounts.</p>
                  </div>
                  <div className="p-5 rounded-xl bg-black/60 border border-white/5 space-y-2">
                    <span className="text-neutral-500">02. Enter Amount</span>
                    <div className="text-white font-semibold">INR Transfer Presets</div>
                    <p className="text-neutral-400 text-[11px]">Quick chips for ₹500, ₹1,000, ₹5,000, or custom amounts up to your authorized limit.</p>
                  </div>
                  <div className="p-5 rounded-xl bg-black/60 border border-white/5 space-y-2">
                    <span className="text-neutral-500">03. Pre-Auth Shield</span>
                    <div className="text-lime font-semibold">Sub-Second Clearance</div>
                    <p className="text-neutral-400 text-[11px]">Autonomous behavioral verification clears legitimate payments instantly.</p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'desktop' && (
              <div className="space-y-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-6 border-b border-white/10">
                  <div>
                    <span className="font-mono text-xs text-lime uppercase tracking-widest">
                      HIGH-PERFORMANCE CLIENT
                    </span>
                    <h3 className="font-display text-2xl font-bold uppercase text-white mt-1">
                      Native Desktop Banking Client
                    </h3>
                  </div>
                  <div className="font-mono text-xs text-neutral-400">
                    Pure Java Swing & Java 2D Engine
                  </div>
                </div>

                <div className="p-6 rounded-xl bg-black/60 border border-white/10 space-y-4 text-xs font-mono">
                  <div className="flex items-center gap-3 text-neutral-300">
                    <CreditCard className="w-5 h-5 text-lime" />
                    <span className="font-semibold text-white">Full-featured native desktop banking matching the web design language.</span>
                  </div>
                  <div className="text-neutral-400 leading-relaxed">
                    Designed for power users and enterprise surveillance. Features hardware-accelerated vector graphics, 3D animated card perspectives, real-time balance trend spline charts, and instant transaction processing.
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-[11px]">
                    <div className="p-3 rounded-lg bg-neutral-900/80 border border-white/5">
                      <span className="text-lime font-semibold">3D Vector Graphics</span>
                      <p className="text-neutral-400 mt-0.5">Smooth perspective rotation and lighting in pure Java 2D.</p>
                    </div>
                    <div className="p-3 rounded-lg bg-neutral-900/80 border border-white/5">
                      <span className="text-lime font-semibold">EDT Thread Safety</span>
                      <p className="text-neutral-400 mt-0.5">Off-thread background processing ensures smooth 60 FPS interactions.</p>
                    </div>
                    <div className="p-3 rounded-lg bg-neutral-900/80 border border-white/5">
                      <span className="text-lime font-semibold">Instant Synchronization</span>
                      <p className="text-neutral-400 mt-0.5">Shared account state seamlessly accessible from web and desktop.</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 5 — FINAL CALL TO ACTION
      ========================================================================= */}
      <section className="py-24 bg-[#080808] relative overflow-hidden">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-lime/[0.05] rounded-full blur-[140px] pointer-events-none" />

        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">
          <span className="font-mono text-xs uppercase tracking-widest text-lime px-2.5 py-1 rounded bg-lime/10 border border-lime/20 inline-block mb-4">
            EXPERIENCE NEXT-GEN BANKING
          </span>
          <h2 className="font-display text-5xl sm:text-7xl uppercase font-bold text-white tracking-tight mb-6">
            SECURE YOUR FINANCIAL FUTURE WITH SAFEPAY.
          </h2>
          <p className="text-neutral-400 text-sm sm:text-base max-w-xl mx-auto mb-10 leading-relaxed">
            Open an account in seconds. Transfer funds instantly, monitor your balances with complete clarity, and enjoy autonomous fraud protection.
          </p>

          <div className="flex flex-wrap justify-center items-center gap-4">
            <Link
              href="/register"
              className="inline-flex items-center gap-3 px-8 py-4 rounded-xl bg-lime hover:bg-lime-hover text-black font-semibold text-sm tracking-wide transition-all shadow-[0_0_30px_rgba(216,255,0,0.4)] hover:shadow-[0_0_40px_rgba(216,255,0,0.6)]"
            >
              <span>GET STARTED NOW</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center gap-2 px-6 py-4 rounded-xl bg-neutral-900 border border-white/10 hover:border-white/30 text-white font-mono text-xs uppercase tracking-wider transition-colors"
            >
              <Lock className="w-3.5 h-3.5 text-lime" />
              <span>ACCESS ACCOUNT</span>
            </Link>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
}
