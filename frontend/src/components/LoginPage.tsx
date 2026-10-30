import React, { useState } from 'react';
import { authService } from '../services/authService';
import { UserProfile } from '../types/auth';

interface LoginPageProps {
  onLoginSuccess: (user: UserProfile) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validation
    if (!username.trim()) {
      setErrorMessage('Please enter your username or email');
      return;
    }
    if (!password) {
      setErrorMessage('Please enter your password');
      return;
    }
    if (password.length < 12) {
      setErrorMessage('Password must be at least 12 characters as per company security policy');
      return;
    }

    setIsLoading(true);
    try {
      const response = await authService.login(username.trim(), password);
      onLoginSuccess({
        id: response.id,
        username: response.username,
        email: response.email,
        role: response.role,
        active: true,
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Invalid username or password';
      setErrorMessage(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.header}>
          <div style={styles.logoBadge}>ESSP</div>
          <h2 style={styles.title}>Employee Self-Service Portal</h2>
          <p style={styles.subtitle}>Sign in with your enterprise credentials</p>
        </div>

        {errorMessage && (
          <div style={styles.errorBox} role="alert">
            <span style={styles.errorIcon}>⚠</span>
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.formGroup}>
            <label htmlFor="username" style={styles.label}>Username or Work Email</label>
            <input
              id="username"
              type="text"
              value={username}
              disabled={isLoading}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. employee1 or employee@essp.local"
              style={styles.input}
              autoComplete="username"
              required
            />
          </div>

          <div style={styles.formGroup}>
            <label htmlFor="password" style={styles.label}>Password (minimum 12 characters)</label>
            <input
              id="password"
              type="password"
              value={password}
              disabled={isLoading}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              style={styles.input}
              autoComplete="current-password"
              required
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            style={isLoading ? { ...styles.button, ...styles.buttonDisabled } : styles.button}
          >
            {isLoading ? 'Verifying credentials...' : 'Sign In'}
          </button>
        </form>

        <div style={styles.footer}>
          <p style={styles.helpText}>Supported roles: Employee, Manager, HR Admin, System Admin</p>
          <p style={styles.securityNote}>🔒 Enterprise-grade encrypted session with token invalidation</p>
        </div>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#0f172a',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    padding: '20px',
  },
  card: {
    width: '100%',
    maxWidth: '440px',
    backgroundColor: '#1e293b',
    borderRadius: '12px',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.4)',
    padding: '36px',
    border: '1px solid #334155',
  },
  header: {
    textAlign: 'center',
    marginBottom: '28px',
  },
  logoBadge: {
    display: 'inline-block',
    padding: '6px 14px',
    backgroundColor: '#3b82f6',
    color: '#ffffff',
    borderRadius: '20px',
    fontWeight: 'bold',
    fontSize: '14px',
    marginBottom: '12px',
    letterSpacing: '1px',
  },
  title: {
    margin: '0 0 6px 0',
    color: '#f8fafc',
    fontSize: '22px',
    fontWeight: 600,
  },
  subtitle: {
    margin: 0,
    color: '#94a3b8',
    fontSize: '14px',
  },
  errorBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    border: '1px solid #ef4444',
    color: '#fca5a5',
    padding: '12px 14px',
    borderRadius: '6px',
    fontSize: '13px',
    marginBottom: '20px',
  },
  errorIcon: {
    fontSize: '16px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '18px',
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  label: {
    color: '#cbd5e1',
    fontSize: '13px',
    fontWeight: 500,
  },
  input: {
    padding: '12px 14px',
    borderRadius: '6px',
    border: '1px solid #475569',
    backgroundColor: '#0f172a',
    color: '#f8fafc',
    fontSize: '14px',
    outline: 'none',
    transition: 'border-color 0.2s',
  },
  button: {
    marginTop: '6px',
    padding: '12px',
    borderRadius: '6px',
    border: 'none',
    backgroundColor: '#3b82f6',
    color: '#ffffff',
    fontSize: '15px',
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  buttonDisabled: {
    opacity: 0.6,
    cursor: 'not-allowed',
  },
  footer: {
    marginTop: '28px',
    textAlign: 'center',
    borderTop: '1px solid #334155',
    paddingTop: '18px',
  },
  helpText: {
    margin: '0 0 4px 0',
    fontSize: '12px',
    color: '#64748b',
  },
  securityNote: {
    margin: 0,
    fontSize: '11px',
    color: '#475569',
  },
};
