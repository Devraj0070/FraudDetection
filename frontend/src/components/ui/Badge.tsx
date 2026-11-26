'use client';

import React from 'react';
import { cn } from '@/lib/utils';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'lime' | 'success' | 'warning' | 'danger' | 'neutral' | 'outline';
  size?: 'sm' | 'md';
  className?: string;
}

export function Badge({
  children,
  variant = 'neutral',
  size = 'sm',
  className,
}: BadgeProps) {
  const variantStyles = {
    lime: 'bg-lime/10 text-lime border-lime/30 shadow-[0_0_10px_rgba(216,255,0,0.15)]',
    success: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    warning: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
    danger: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
    neutral: 'bg-neutral-800 text-neutral-300 border-neutral-700',
    outline: 'bg-transparent text-neutral-400 border-neutral-800',
  };

  const sizeStyles = {
    sm: 'text-[10px] px-2 py-0.5 tracking-wider',
    md: 'text-xs px-2.5 py-1 tracking-wide',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 font-mono uppercase font-medium rounded-full border',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
    >
      {children}
    </span>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const normalized = (status || '').toUpperCase();
  if (normalized === 'APPROVED' || normalized === 'ACTIVE' || normalized === 'RESOLVED') {
    return <Badge variant="success">{status}</Badge>;
  }
  if (normalized === 'PENDING_REVIEW' || normalized === 'PENDING' || normalized === 'REVIEW') {
    return <Badge variant="warning">{status.replace('_', ' ')}</Badge>;
  }
  if (normalized === 'DECLINED' || normalized === 'BLOCKED' || normalized === 'REJECTED' || normalized === 'HIGH') {
    return <Badge variant="danger">{status}</Badge>;
  }
  if (normalized === 'MEDIUM') {
    return <Badge variant="warning">MED RISK</Badge>;
  }
  if (normalized === 'LOW') {
    return <Badge variant="lime">LOW RISK</Badge>;
  }
  return <Badge variant="neutral">{status}</Badge>;
}
