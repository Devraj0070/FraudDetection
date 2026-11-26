'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/lib/auth-context';
import { Menu, X, ArrowRight, User, LogOut } from 'lucide-react';

export function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-[#080808]/85 backdrop-blur-md border-b border-white/[0.06]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link href="/" className="flex items-center gap-2 group">
          <div className="w-8 h-8 rounded-lg bg-black border border-white/10 flex items-center justify-center group-hover:border-lime transition-colors">
            <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
          </div>
          <span className="font-display text-2xl font-bold tracking-wider text-white">
            SAFEPAY
          </span>
          <span className="font-mono text-[10px] tracking-widest text-lime px-1.5 py-0.5 rounded bg-lime/10 border border-lime/20 hidden sm:inline-block">
            DIGITAL BANK
          </span>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-8 font-mono text-xs uppercase tracking-wider text-neutral-400">
          <a href="#services" className="hover:text-lime transition-colors">
            Services
          </a>
          <a href="#security" className="hover:text-lime transition-colors">
            Security & Trust
          </a>
          <a href="#experience" className="hover:text-lime transition-colors">
            Experience
          </a>
          <Link href="/transfer" className="hover:text-lime transition-colors">
            Transfers
          </Link>
        </nav>

        {/* Desktop Action Buttons */}
        <div className="hidden md:flex items-center gap-4">
          {user ? (
            <div className="flex items-center gap-3">
              <Link
                href={isAdmin ? '/admin' : '/dashboard'}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-neutral-900 border border-white/10 text-white font-mono text-xs hover:border-lime transition-colors"
              >
                <User className="w-3.5 h-3.5 text-lime" />
                <span>{user.username}</span>
                <span className="text-[10px] text-lime px-1 rounded bg-lime/10">
                  {user.role}
                </span>
              </Link>
              <button
                onClick={logout}
                className="p-2 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-900 transition-colors"
                title="Sign Out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link
                href="/login"
                className="font-mono text-xs text-neutral-300 hover:text-white px-3 py-2 transition-colors uppercase tracking-wider"
              >
                Sign In
              </Link>
              <Link
                href="/register"
                className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-lime hover:bg-lime-hover text-black font-semibold text-xs tracking-wide transition-all shadow-[0_0_15px_rgba(216,255,0,0.3)] hover:shadow-[0_0_25px_rgba(216,255,0,0.5)]"
              >
                <span>OPEN ACCOUNT</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          )}
        </div>

        {/* Mobile Hamburger Toggle */}
        <div className="flex md:hidden">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 text-neutral-400 hover:text-white"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-[#0c0c0e] border-b border-white/10 px-6 py-6 flex flex-col gap-4 font-mono text-sm uppercase">
          <a
            href="#services"
            onClick={() => setMobileMenuOpen(false)}
            className="text-neutral-300 hover:text-lime py-1"
          >
            Services
          </a>
          <a
            href="#security"
            onClick={() => setMobileMenuOpen(false)}
            className="text-neutral-300 hover:text-lime py-1"
          >
            Security & Trust
          </a>
          <a
            href="#experience"
            onClick={() => setMobileMenuOpen(false)}
            className="text-neutral-300 hover:text-lime py-1"
          >
            Experience
          </a>
          <Link
            href="/transfer"
            onClick={() => setMobileMenuOpen(false)}
            className="text-neutral-300 hover:text-lime py-1"
          >
            Transfers
          </Link>

          <div className="pt-4 border-t border-white/10 flex flex-col gap-3">
            {user ? (
              <>
                <Link
                  href={isAdmin ? '/admin' : '/dashboard'}
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-white py-1 flex items-center justify-between"
                >
                  <span>{user.username} ({user.role})</span>
                </Link>
                <button
                  onClick={() => {
                    logout();
                    setMobileMenuOpen(false);
                  }}
                  className="text-rose-400 text-left py-1"
                >
                  Sign Out
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-neutral-300 hover:text-white py-1"
                >
                  Sign In
                </Link>
                <Link
                  href="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="px-4 py-2.5 rounded-lg bg-lime text-black font-semibold text-center tracking-wide"
                >
                  OPEN ACCOUNT
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
