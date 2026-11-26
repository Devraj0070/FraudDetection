import React from 'react';
import Link from 'next/link';

export function Footer() {
  return (
    <footer className="bg-[#050506] border-t border-white/[0.08] pt-16 pb-12 text-neutral-400 font-sans">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 mb-12">
          {/* Brand Info */}
          <div className="md:col-span-1">
            <div className="flex items-center gap-2 mb-4">
              <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
              <span className="font-display text-2xl font-bold tracking-wider text-white">
                SAFEPAY
              </span>
            </div>
            <p className="text-xs text-neutral-400 leading-relaxed mb-4">
              Next-generation digital banking platform with autonomous transaction protection. Seamless payments, instant transfers, and zero fraud liability.
            </p>
            <div className="font-mono text-[10px] text-neutral-500 uppercase tracking-wider">
              SMARTER TRANSACTIONS · SAFER PAYMENTS
            </div>
          </div>

          {/* Product & Services */}
          <div>
            <h4 className="font-mono text-xs uppercase tracking-widest text-white mb-4">
              Banking Services
            </h4>
            <ul className="space-y-2 text-xs font-mono">
              <li>
                <Link href="/dashboard" className="hover:text-lime transition-colors">
                  Account Overview
                </Link>
              </li>
              <li>
                <Link href="/transfer" className="hover:text-lime transition-colors">
                  Instant Money Transfers
                </Link>
              </li>
              <li>
                <Link href="/transactions" className="hover:text-lime transition-colors">
                  Transaction Ledger
                </Link>
              </li>
              <li>
                <Link href="/register" className="hover:text-lime transition-colors">
                  Open New Account
                </Link>
              </li>
            </ul>
          </div>

          {/* Security & Guarantees */}
          <div>
            <h4 className="font-mono text-xs uppercase tracking-widest text-white mb-4">
              Security Guarantee
            </h4>
            <ul className="space-y-2 text-xs font-mono text-neutral-400">
              <li>Zero Fraud Liability Protection</li>
              <li>Real-Time Pre-Auth Screening</li>
              <li>Escrow Dispute Holds</li>
              <li>256-Bit Financial Encryption</li>
              <li>24/7 Account Surveillance</li>
            </ul>
          </div>

          {/* Protection Status */}
          <div>
            <h4 className="font-mono text-xs uppercase tracking-widest text-white mb-4">
              Security Status
            </h4>
            <div className="p-4 rounded-xl bg-neutral-950 border border-white/10 space-y-2 text-xs font-mono">
              <div className="flex justify-between items-center">
                <span className="text-neutral-400">Protection Shield</span>
                <span className="inline-flex items-center gap-1.5 text-lime">
                  <span className="w-1.5 h-1.5 rounded-full bg-lime animate-ping" />
                  ACTIVE
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-neutral-400">Clearance Speed</span>
                <span className="text-lime font-semibold">&lt; 1 Second</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-neutral-400">Encryption</span>
                <span className="text-white">TLS 1.3 / AES-256</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-neutral-400">Account Vault</span>
                <span className="text-white">Protected</span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-8 border-t border-white/[0.06] flex flex-col md:flex-row justify-between items-center gap-4 text-xs font-mono text-neutral-500">
          <div>
            © {new Date().getFullYear()} SafePay Financial Technologies Inc. All rights reserved.
          </div>
          <div className="flex gap-6">
            <span className="text-neutral-400">All Accounts Insured</span>
            <span className="text-neutral-400">Zero Unauthorized Deductions</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
