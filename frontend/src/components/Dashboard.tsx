import React from 'react';
import { UserProfile } from '../types/auth';

interface DashboardProps {
  user: UserProfile;
  onLogout: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
  const getBadgeColor = (role: string) => {
    switch (role) {
      case 'SYSTEM_ADMIN': return '#ef4444';
      case 'HR_ADMIN': return '#a855f7';
      case 'MANAGER': return '#f59e0b';
      default: return '#10b981';
    }
  };

  return (
    <div style={styles.container}>
      <header style={styles.navbar}>
        <div style={styles.navBrand}>
          <span style={styles.logoBadge}>ESSP</span>
          <span style={styles.navTitle}>Employee Self-Service Portal</span>
        </div>
        <div style={styles.navUser}>
          <span style={styles.welcomeText}>
            Logged in as <strong>{user.username}</strong>
          </span>
          <span style={{ ...styles.roleBadge, backgroundColor: getBadgeColor(user.role) }}>
            {user.role}
          </span>
          <button onClick={onLogout} style={styles.logoutBtn}>
            Logout
          </button>
        </div>
      </header>

      <main style={styles.content}>
        <div style={styles.card}>
          <h2 style={styles.sectionTitle}>Authentication & Session Verified</h2>
          <p style={styles.desc}>
            Member 64 Authentication and Authorization module is active. Real JWT session established.
          </p>

          <div style={styles.infoGrid}>
            <div style={styles.infoBox}>
              <span style={styles.infoLabel}>User ID:</span>
              <span style={styles.infoValue}>{user.id}</span>
            </div>
            <div style={styles.infoBox}>
              <span style={styles.infoLabel}>Username:</span>
              <span style={styles.infoValue}>{user.username}</span>
            </div>
            <div style={styles.infoBox}>
              <span style={styles.infoLabel}>Email Address:</span>
              <span style={styles.infoValue}>{user.email}</span>
            </div>
            <div style={styles.infoBox}>
              <span style={styles.infoLabel}>Role Assigned:</span>
              <span style={styles.infoValue}>{user.role}</span>
            </div>
          </div>
        </div>

        <div style={styles.integrationCard}>
          <h3 style={styles.subTitle}>Cross-Member Integration Points</h3>
          <ul style={styles.list}>
            <li><strong>Member 70 (Employee Profile & Address):</strong> Protected with <code>hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')</code></li>
            <li><strong>Member 96 (Leave Management):</strong> Submissions protected by Employee role; approvals require <code>hasAnyRole('MANAGER', 'HR_ADMIN')</code></li>
            <li><strong>Member 114 (Payslip & Audit):</strong> Payslip retrieval restricted to owner; security audit logs dispatched to <code>SecurityAuditService</code></li>
          </ul>
        </div>
      </main>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#0f172a',
    color: '#f8fafc',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
  },
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 32px',
    backgroundColor: '#1e293b',
    borderBottom: '1px solid #334155',
  },
  navBrand: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  logoBadge: {
    padding: '4px 10px',
    backgroundColor: '#3b82f6',
    borderRadius: '12px',
    fontWeight: 'bold',
    fontSize: '13px',
  },
  navTitle: {
    fontSize: '18px',
    fontWeight: 600,
  },
  navUser: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  },
  welcomeText: {
    fontSize: '14px',
    color: '#cbd5e1',
  },
  roleBadge: {
    padding: '4px 10px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: 600,
  },
  logoutBtn: {
    padding: '8px 16px',
    borderRadius: '6px',
    border: '1px solid #475569',
    backgroundColor: 'transparent',
    color: '#f8fafc',
    cursor: 'pointer',
    fontSize: '13px',
    transition: 'background 0.2s',
  },
  content: {
    maxWidth: '900px',
    margin: '40px auto',
    padding: '0 20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
  },
  card: {
    backgroundColor: '#1e293b',
    borderRadius: '10px',
    padding: '28px',
    border: '1px solid #334155',
  },
  integrationCard: {
    backgroundColor: '#1e293b',
    borderRadius: '10px',
    padding: '24px',
    border: '1px solid #334155',
  },
  sectionTitle: {
    margin: '0 0 8px 0',
    fontSize: '20px',
  },
  subTitle: {
    margin: '0 0 12px 0',
    fontSize: '16px',
    color: '#38bdf8',
  },
  desc: {
    color: '#94a3b8',
    fontSize: '14px',
    margin: '0 0 24px 0',
  },
  infoGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px',
  },
  infoBox: {
    backgroundColor: '#0f172a',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #334155',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  infoLabel: {
    fontSize: '12px',
    color: '#94a3b8',
  },
  infoValue: {
    fontSize: '15px',
    fontWeight: 500,
    color: '#f8fafc',
  },
  list: {
    margin: 0,
    paddingLeft: '20px',
    color: '#cbd5e1',
    lineHeight: 1.8,
    fontSize: '14px',
  },
};
