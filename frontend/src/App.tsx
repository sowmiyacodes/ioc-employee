import React, { useEffect, useState } from 'react';
import { Dashboard } from './components/Dashboard';
import { HomePage } from './components/HomePage';
import { LoginPage } from './components/LoginPage';
import { authService } from './services/authService';
import { UserProfile } from './types/auth';
import { canAccessPath, roleHome } from './routes';

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [path, setPath] = useState(window.location.pathname);
  const [isInitializing, setIsInitializing] = useState(true);
  const [sessionError, setSessionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const navigate = (nextPath: string, replace = false) => {
    if (replace) window.history.replaceState({}, '', nextPath);
    else window.history.pushState({}, '', nextPath);
    setPath(nextPath);
  };

  const verifySession = async () => {
    setIsInitializing(true);
    setSessionError(null);
    try {
      const user = await authService.getCurrentUser();
      setCurrentUser(user);
      if (user && (window.location.pathname === '/' || window.location.pathname === '/login')) {
        navigate(roleHome[user.role], true);
      }
    } catch (error: unknown) {
      setSessionError(error instanceof Error ? error.message : 'Unable to verify your session.');
    } finally {
      setIsInitializing(false);
    }
  };

  useEffect(() => {
    void verifySession();
    const handlePopState = () => setPath(window.location.pathname);
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  useEffect(() => {
    if (isInitializing || sessionError) return;
    if (!currentUser) {
      if (path !== '/' && path !== '/login') navigate('/', true);
      return;
    }
    if (path === '/' || path === '/login' || !canAccessPath(currentUser.role, path)) {
      navigate(roleHome[currentUser.role], true);
    }
  }, [currentUser, isInitializing, path, sessionError]);

  const handleLoginSuccess = (user: UserProfile) => {
    setCurrentUser(user);
    setNotice(null);
    navigate(roleHome[user.role], true);
  };

  const handleLogout = async () => {
    setNotice(null);
    try {
      await authService.logout();
    } catch {
      authService.clearSession();
      setNotice('You have been signed out on this device. The server could not be notified, so the token may remain valid until it expires.');
    } finally {
      setCurrentUser(null);
      navigate('/', true);
    }
  };

  if (isInitializing) {
    return <div className="loading-screen"><div><div className="spinner" /><p>Verifying your session…</p></div></div>;
  }

  if (sessionError) {
    return (
      <main className="session-error">
        <div className="login-card">
          <h1>Session verification failed</h1>
          <div className="notice notice-error" role="alert">{sessionError}</div>
          <button className="button button-primary" onClick={() => void verifySession()}>Try again</button>
          <button className="button button-secondary" onClick={() => { authService.clearSession(); setSessionError(null); setCurrentUser(null); navigate('/', true); }}>Sign out on this device</button>
        </div>
      </main>
    );
  }

  if (currentUser) {
    return <Dashboard user={currentUser} path={path} onNavigate={navigate} onLogout={handleLogout} />;
  }

  if (path === '/login') {
    return <LoginPage onLoginSuccess={handleLoginSuccess} onBack={() => navigate('/')} />;
  }

  return <HomePage onSignIn={() => navigate('/login')} notice={notice} />;
};

export default App;
