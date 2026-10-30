import React from 'react';

interface Props {
  onSignIn: () => void;
  notice?: string | null;
}

const features = [
  {
    icon: 'P',
    title: 'Profile management',
    description: 'Review your employment details and keep your residential and mailing address up to date.',
  },
  {
    icon: 'L',
    title: 'Leave management',
    description: 'View your leave balance, submit a request, and follow its status from one place.',
  },
  {
    icon: 'P',
    title: 'Payslip access',
    description: 'View your available pay periods and securely download your own PDF payslips.',
  },
  {
    icon: 'S',
    title: 'Secure self-service',
    description: 'Access portal features protected by your account and assigned role.',
  },
];

export const HomePage: React.FC<Props> = ({ onSignIn, notice }) => (
  <>
    <header className="site-header">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true">ESSP</span>
        <span className="brand-name">Employee Self-Service Portal</span>
      </div>
      <button className="button button-primary" onClick={onSignIn}>Sign in</button>
    </header>
    <main>
      {notice && <div className="home-notice notice notice-info" role="status">{notice}</div>}
      <section className="home-hero">
        <span className="eyebrow">A simpler way to manage your work essentials</span>
        <h1>Your employee services, all in one place.</h1>
        <p>
          Manage your profile, keep your details current, and access your leave
          and payslips through a secure employee portal.
        </p>
        <button className="button button-primary" onClick={onSignIn}>Sign in to your portal <span aria-hidden="true">→</span></button>
      </section>
      <section className="feature-section" aria-labelledby="features-title">
        <h2 className="section-heading" id="features-title">Self-service made straightforward</h2>
        <div className="feature-grid">
          {features.map((feature) => (
            <article className="feature-card" key={feature.title}>
              <span className="feature-icon" aria-hidden="true">{feature.icon}</span>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
    <footer className="site-footer">Employee Self-Service Portal · Secure access for employees</footer>
  </>
);
