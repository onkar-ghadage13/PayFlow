import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../services/api.js';

const AuthContext = createContext(null);

function storedUser() {
  try { return JSON.parse(localStorage.getItem('payflow-user') || sessionStorage.getItem('payflow-user')); }
  catch { return null; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storedUser);

  useEffect(() => {
    const expireSession = () => {
      localStorage.removeItem('payflow-token');
      localStorage.removeItem('payflow-user');
      sessionStorage.removeItem('payflow-token');
      sessionStorage.removeItem('payflow-user');
      setUser(null);
    };
    window.addEventListener('payflow:session-expired', expireSession);
    return () => window.removeEventListener('payflow:session-expired', expireSession);
  }, []);

  const login = credentials => api('/auth/login', { method: 'POST', body: JSON.stringify(credentials) });
  const register = details => api('/auth/register', { method: 'POST', body: JSON.stringify(details) });
  const authenticate = (auth, remember = true) => {
    const store = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    other.removeItem('payflow-token');
    other.removeItem('payflow-user');
    store.setItem('payflow-token', auth.token);
    store.setItem('payflow-user', JSON.stringify(auth.user));
    setUser(auth.user);
  };
  const logout = () => {
    localStorage.removeItem('payflow-token');
    localStorage.removeItem('payflow-user');
    sessionStorage.removeItem('payflow-token');
    sessionStorage.removeItem('payflow-user');
    setUser(null);
  };

  const value = useMemo(() => ({ user, login, register, authenticate, logout }), [user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
