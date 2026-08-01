import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, setAccessToken } from './api';
import type { User } from './types';

interface AuthContextValue {
  user: User | null;
  restoring: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = sessionStorage.getItem('board.user');
    return stored ? JSON.parse(stored) as User : null;
  });
  const [restoring, setRestoring] = useState(true);

  useEffect(() => {
    api.restore().then((ok) => {
      if (!ok) {
        sessionStorage.removeItem('board.user');
        setUser(null);
      }
    }).finally(() => setRestoring(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    restoring,
    login: async (email, password) => {
      const response = await api.login(email, password);
      setAccessToken(response.accessToken);
      const nextUser: User = { id: response.id, email: response.email, nickName: response.nickName, role: response.role };
      sessionStorage.setItem('board.user', JSON.stringify(nextUser));
      setUser(nextUser);
    },
    logout: async () => {
      try { await api.logout(); } finally {
        setAccessToken(null);
        sessionStorage.removeItem('board.user');
        setUser(null);
      }
    },
  }), [user, restoring]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
