// @ts-ignore
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, setAccessToken } from './api';
import type { User } from './types';

interface AuthContextValue {
  user: User | null;
  restoring: boolean;
  login: (email: string, password: string) => Promise<void>;
  restoreSession: () => Promise<boolean>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = sessionStorage.getItem('board.user');
    return stored ? JSON.parse(stored) as User : null;
  });
  const [restoring, setRestoring] = useState(true);

  const applySession = useCallback((response: User & { accessToken: string }) => {
    setAccessToken(response.accessToken);
    const nextUser: User = { id: response.id, email: response.email, nickName: response.nickName, role: response.role };
    sessionStorage.setItem('board.user', JSON.stringify(nextUser));
    setUser(nextUser);
  }, []);

  useEffect(() => {
    api.restore().then((response) => {
      if (response) {
        applySession(response);
      } else {
        sessionStorage.removeItem('board.user');
        setUser(null);
      }
    }).finally(() => setRestoring(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.login(email, password);
    applySession(response);
  }, [applySession]);

  const restoreSession = useCallback(async () => {
    const response = await api.restore();
    if (!response) return false;
    applySession(response);
    return true;
  }, [applySession]);

  const logout = useCallback(async () => {
      try { await api.logout(); } finally {
        setAccessToken(null);
        sessionStorage.removeItem('board.user');
        setUser(null);
      }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user, restoring, login, restoreSession, logout,
  }), [user, restoring, login, restoreSession, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
