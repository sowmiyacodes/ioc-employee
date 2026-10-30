import React, { useState } from 'react';
import { authService } from '../services/authService';
import { UserProfile } from '../types/auth';

interface LoginPageProps {
  onLoginSuccess: (user: UserProfile) => void;
  onBack: () => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess, onBack }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    if (password.length < 12) {
      setErrorMessage('Password must be at least 12 characters.');
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
    } catch (error: unknown) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to sign in. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <header className="site-header">
        <button className="brand text-button" onClick={onBack} aria-label="Return to portal home">
          <span className="brand-mark" aria-hidden="true">ESSP</span>
          <span className="brand-name">Employee Self-Service Portal</span>
        </button>
        <button className="button button-secondary" onClick={onBack}>Back to home</button>
      </header>
      <main className="login-wrap">
        <section className="login-aside">
          <span className="eyebrow">Employee services, made simple</span>
          <h1>Welcome back to your workspace.</h1>
          <p>Sign in to securely access your employee profile and the services available to your role.</p>
        </section>
        <section className="login-main" aria-labelledby="login-title">
          <div className="login-card">
            <h2 id="login-title">Sign in</h2>
            <p>Use your employee username or work email.</p>
            {errorMessage && <div className="notice notice-error" role="alert">{errorMessage}</div>}
            <form onSubmit={handleSubmit}>
              <label className="field" htmlFor="username">
                Username or work email
                <input
                  id="username"
                  name="username"
                  type="text"
                  autoComplete="username"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  required
                  disabled={isLoading}
                />
              </label>
              <label className="field" htmlFor="password">
                Password
                <span className="password-row">
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    minLength={12}
                    required
                    disabled={isLoading}
                  />
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => setShowPassword((visible) => !visible)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    aria-pressed={showPassword}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </span>
              </label>
              <button className="button button-primary" type="submit" disabled={isLoading}>
                {isLoading ? 'Signing in…' : 'Sign in securely'}
              </button>
            </form>
            <p className="login-role-note">
              Your role determines which portal features are available after sign-in.
            </p>
          </div>
        </section>
      </main>
    </>
  );
};
