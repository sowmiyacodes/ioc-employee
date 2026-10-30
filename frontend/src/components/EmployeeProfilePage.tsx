import React, { useEffect, useState } from 'react';
import { employeeService } from '../services/employeeService';
import { Address, EmployeeProfile } from '../types/employee';

interface Props {
  onClose: () => void;
}

const emptyAddress: Address = {
  residentialStreet: '', residentialCity: '', residentialState: '',
  residentialPostalCode: '', residentialCountry: '',
  mailingStreet: '', mailingCity: '', mailingState: '',
  mailingPostalCode: '', mailingCountry: '',
};

const addressFields: Array<{ key: keyof Address; label: string }> = [
  { key: 'residentialStreet', label: 'Residential street' },
  { key: 'residentialCity', label: 'Residential city' },
  { key: 'residentialState', label: 'Residential state' },
  { key: 'residentialPostalCode', label: 'Residential postal code' },
  { key: 'residentialCountry', label: 'Residential country' },
  { key: 'mailingStreet', label: 'Mailing street' },
  { key: 'mailingCity', label: 'Mailing city' },
  { key: 'mailingState', label: 'Mailing state' },
  { key: 'mailingPostalCode', label: 'Mailing postal code' },
  { key: 'mailingCountry', label: 'Mailing country' },
];

export const EmployeeProfilePage: React.FC<Props> = ({ onClose }) => {
  const [profile, setProfile] = useState<EmployeeProfile | null>(null);
  const [form, setForm] = useState<Address>(emptyAddress);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    employeeService.getMyProfile()
      .then((value) => {
        setProfile(value);
        setForm(value.address || emptyAddress);
      })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'Unable to load profile.'))
      .finally(() => setLoading(false));
  }, []);

  const updateField = (key: keyof Address, value: string) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const copyResidential = () => {
    setForm((current) => ({
      ...current,
      mailingStreet: current.residentialStreet,
      mailingCity: current.residentialCity,
      mailingState: current.residentialState,
      mailingPostalCode: current.residentialPostalCode,
      mailingCountry: current.residentialCountry,
    }));
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    const missing = addressFields.find(({ key }) => !form[key].trim());
    if (missing) {
      setError(`${missing.label} is required.`);
      return;
    }
    setSaving(true);
    try {
      const updated = await employeeService.updateAddress(form);
      setProfile(updated);
      setForm(updated.address || emptyAddress);
      setSuccess('Address updated successfully.');
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to update address.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={styles.card}>Loading employee profile...</div>;
  if (!profile) return <div style={styles.card}><p style={styles.error}>{error}</p><button onClick={onClose}>Back</button></div>;

  return (
    <div style={styles.card}>
      <div style={styles.heading}>
        <div><h2 style={styles.title}>My Employee Profile</h2><p style={styles.muted}>Employment details are read-only.</p></div>
        <button onClick={onClose} style={styles.secondaryButton}>Back to dashboard</button>
      </div>
      {error && <p role="alert" style={styles.error}>{error}</p>}
      {success && <p role="status" style={styles.success}>{success}</p>}
      <div style={styles.infoGrid}>
        {[
          ['Employee ID', profile.employeeId],
          ['Legal name', profile.legalName],
          ['Department', profile.department],
          ['Designation', profile.designation],
          ['Joining date', profile.joiningDate],
          ['Manager', profile.managerName || 'Not assigned'],
        ].map(([label, value]) => <div style={styles.infoBox} key={label}><span style={styles.label}>{label}</span><strong>{value}</strong></div>)}
      </div>
      <form onSubmit={save}>
        <div style={styles.addressHeading}><h3 style={styles.subtitle}>Editable address</h3><button type="button" onClick={copyResidential} style={styles.secondaryButton}>Copy residential to mailing</button></div>
        <div style={styles.formGrid}>
          {addressFields.map(({ key, label }) => (
            <label key={key} style={styles.field}>{label}
              <input value={form[key]} onChange={(event) => updateField(key, event.target.value)} maxLength={key.includes('Street') ? 200 : 100} disabled={saving} />
            </label>
          ))}
        </div>
        <button type="submit" disabled={saving} style={styles.primaryButton}>{saving ? 'Saving...' : 'Save address'}</button>
      </form>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  card: { backgroundColor: '#1e293b', borderRadius: '10px', padding: '28px', border: '1px solid #334155' },
  heading: { display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: '16px' },
  title: { margin: 0, fontSize: '22px' },
  subtitle: { color: '#38bdf8', margin: 0 },
  muted: { color: '#94a3b8', fontSize: '13px' },
  infoGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', margin: '22px 0' },
  infoBox: { backgroundColor: '#0f172a', padding: '14px', borderRadius: '8px', display: 'flex', flexDirection: 'column', gap: '5px' },
  label: { color: '#94a3b8', fontSize: '12px' },
  addressHeading: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '24px 0 14px' },
  formGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px' },
  field: { display: 'flex', flexDirection: 'column', gap: '6px', color: '#cbd5e1', fontSize: '13px' },
  input: {},
  primaryButton: { marginTop: '22px', padding: '11px 18px', border: 0, borderRadius: '6px', backgroundColor: '#3b82f6', color: 'white', cursor: 'pointer' },
  secondaryButton: { padding: '8px 12px', border: '1px solid #475569', borderRadius: '6px', backgroundColor: 'transparent', color: '#f8fafc', cursor: 'pointer' },
  error: { color: '#fca5a5', backgroundColor: 'rgba(239,68,68,.15)', padding: '10px', borderRadius: '6px' },
  success: { color: '#86efac', backgroundColor: 'rgba(34,197,94,.15)', padding: '10px', borderRadius: '6px' },
};
