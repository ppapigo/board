// @ts-ignore
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, setAccessToken } from './api';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = sessionStorage.getItem('board.user');
        return stored ? JSON.parse(stored) : null;
    });
    const [restoring, setRestoring] = useState(true);
    const applySession = useCallback((response) => {
        setAccessToken(response.accessToken);
        const nextUser = { id: response.id, email: response.email, nickName: response.nickName, role: response.role };
        sessionStorage.setItem('board.user', JSON.stringify(nextUser));
        setUser(nextUser);
    }, []);
    useEffect(() => {
        api.restore().then((response) => {
            if (response) {
                applySession(response);
            }
            else {
                sessionStorage.removeItem('board.user');
                setUser(null);
            }
        }).finally(() => setRestoring(false));
    }, []);
    const login = useCallback(async (email, password) => {
        const response = await api.login(email, password);
        applySession(response);
    }, [applySession]);
    const restoreSession = useCallback(async () => {
        const response = await api.restore();
        if (!response)
            return false;
        applySession(response);
        return true;
    }, [applySession]);
    const logout = useCallback(async () => {
        try {
            await api.logout();
        }
        finally {
            setAccessToken(null);
            sessionStorage.removeItem('board.user');
            setUser(null);
        }
    }, []);
    const value = useMemo(() => ({
        user, restoring, login, restoreSession, logout,
    }), [user, restoring, login, restoreSession, logout]);
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
    const value = useContext(AuthContext);
    if (!value)
        throw new Error('useAuth must be used inside AuthProvider');
    return value;
}
