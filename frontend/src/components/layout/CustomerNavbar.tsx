'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import {
  CreditCard,
  Send,
  History,
  LogOut,
  User,
  Shield,
  Menu,
  X,
} from 'lucide-react';

export function CustomerNavbar() {
  const { user, logout, isAdmin } = useAuth();
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { label: 'Overview', href: '/dashboard', icon: CreditCard },
    { label: 'Transfer & Pay', href: '/transfer', icon: Send },
    { label: 'History', href: '/transactions', icon: History },
  ];

  return (
    <header className="bg-white border-b border-neutral-200 sticky top-0 z-40 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-6">
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-black flex items-center justify-center text-lime shadow-sm">
              <span className="w-2.5 h-2.5 rounded-full bg-lime shadow-[0_0_8px_#D8FF00]" />
            </div>
            <span className="font-display text-2xl font-bold tracking-wider text-black">
              SAFEPAY
            </span>
            <span className="font-mono text-[10px] tracking-widest text-neutral-600 px-2 py-0.5 rounded bg-neutral-100 border border-neutral-200 uppercase font-medium">
              BANKING
            </span>
          </Link>

          {/* Desktop Nav Links */}
          <nav className="hidden md:flex items-center gap-1 font-mono text-xs">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-lg transition-colors font-medium ${
                    isActive
                      ? 'bg-neutral-100 text-black font-semibold'
                      : 'text-neutral-600 hover:text-black hover:bg-neutral-50'
                  }`}
                >
                  <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-black' : 'text-neutral-500'}`} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Info & Actions */}
        <div className="flex items-center gap-3">
          {isAdmin && (
            <Link
              href="/admin"
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-black text-lime font-mono text-xs hover:bg-neutral-900 transition-colors shadow-sm"
            >
              <Shield className="w-3.5 h-3.5" />
              <span>ADMIN RADAR</span>
            </Link>
          )}

          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-neutral-50 border border-neutral-200 font-mono text-xs text-neutral-800">
            <User className="w-3.5 h-3.5 text-neutral-500" />
            <span className="font-semibold">{user?.username || 'Client'}</span>
            {user?.accountNumber && (
              <span className="text-[11px] text-neutral-500 bg-neutral-200/60 px-1.5 py-0.5 rounded">
                {user.accountNumber}
              </span>
            )}
          </div>

          <button
            onClick={logout}
            className="hidden sm:flex p-2 rounded-lg text-neutral-600 hover:text-black hover:bg-neutral-100 transition-colors"
            title="Sign Out"
          >
            <LogOut className="w-4 h-4" />
          </button>

          {/* Mobile Menu Toggle Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-lg text-neutral-700 hover:bg-neutral-100 transition-colors"
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-neutral-200 bg-white px-4 py-4 space-y-3 font-mono text-xs shadow-lg animate-in slide-in-from-top-2 duration-150">
          <div className="flex items-center justify-between pb-3 border-b border-neutral-100">
            <div className="flex items-center gap-2 text-neutral-800">
              <User className="w-4 h-4 text-neutral-500" />
              <span className="font-semibold">{user?.username || 'Client'}</span>
            </div>
            {user?.accountNumber && (
              <span className="text-[11px] text-neutral-500 bg-neutral-100 px-2 py-0.5 rounded">
                Acct: {user.accountNumber}
              </span>
            )}
          </div>

          <div className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-neutral-100 text-black font-semibold'
                      : 'text-neutral-600 hover:bg-neutral-50 hover:text-black'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </div>

          {isAdmin && (
            <div className="pt-2 border-t border-neutral-100">
              <Link
                href="/admin"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center justify-between px-3 py-2.5 rounded-lg bg-black text-lime font-semibold"
              >
                <div className="flex items-center gap-2">
                  <Shield className="w-4 h-4" />
                  <span>Admin Surveillance</span>
                </div>
                <span className="text-[10px] bg-lime/20 text-lime px-1.5 py-0.5 rounded">ROLE_ADMIN</span>
              </Link>
            </div>
          )}

          <div className="pt-2 border-t border-neutral-100">
            <button
              onClick={() => {
                setMobileMenuOpen(false);
                logout();
              }}
              className="w-full flex items-center gap-2 px-3 py-2 text-rose-600 hover:bg-rose-50 rounded-lg transition-colors font-semibold"
            >
              <LogOut className="w-4 h-4" />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
