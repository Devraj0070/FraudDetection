'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { api } from './api';
import { AuthResponse, LoginRequest, RegisterRequest } from './types';

interface AuthContextType {
  user: AuthResponse | null;
  loading: boolean;
  isAdmin: boolean;
  login: (data: LoginRequest) => Promise<AuthResponse>;
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  const refreshSession = useCallback(async () => {
    try {
      const me = await api.auth.me();
      if (me.success && me.username) {
        setUser(me);
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Ensure CSRF token is warm on startup
    api.auth.csrf().catch(() => {});
    refreshSession();
  }, [refreshSession]);

  const login = async (data: LoginRequest): Promise<AuthResponse> => {
    setLoading(true);
    try {
      const res = await api.auth.login(data);
      if (res.success) {
        setUser(res);
      }
      return res;
    } finally {
      setLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    setLoading(true);
    try {
      return await api.auth.register(data);
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.auth.logout();
    } catch (e) {
      console.warn('Logout API error:', e);
    } finally {
      setUser(null);
      router.push('/login');
    }
  };

  const isAdmin = Boolean(
    user?.role && (user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' || user.role.includes('ADMIN'))
  );

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAdmin,
        login,
        register,
        logout,
        refreshSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}