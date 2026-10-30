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

const addressFields: Array<{ key: keyof Address; label: string; maxLength: number }> = [
  { key: 'residentialStreet', label: 'Street address', maxLength: 200 },
  { key: 'residentialCity', label: 'City', maxLength: 100 },
  { key: 'residentialState', label: 'State or province', maxLength: 100 },
  { key: 'residentialPostalCode', label: 'Postal code', maxLength: 20 },
  { key: 'residentialCountry', label: 'Country', maxLength: 100 },
  { key: 'mailingStreet', label: 'Street address', maxLength: 200 },
  { key: 'mailingCity', label: 'City', maxLength: 100 },
  { key: 'mailingState', label: 'State or province', maxLength: 100 },
  { key: 'mailingPostalCode', label: 'Postal code', maxLength: 20 },
  { key: 'mailingCountry', label: 'Country', maxLength: 100 },
];

export const EmployeeProfilePage: React.FC<Props> = ({ onClose }) => {
  const [profile, setProfile] = useState<EmployeeProfile | null>(null);
  const [form, setForm] = useState<Address>(emptyAddress);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    employeeService.getMyProfile()
      .then((value) => {
        if (!active) return;
        setProfile(value);
        setForm(value.address || emptyAddress);
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load your profile.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const updateField = (key: keyof Address, value: string) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const beginEditing = () => {
    setForm(profile?.address || emptyAddress);
    setError(null);
    setSuccess(null);
    setEditing(true);
  };

  const cancelEditing = () => {
    setForm(profile?.address || emptyAddress);
    setError(null);
    setEditing(false);
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

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
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
      setEditing(false);
      setSuccess('Your address has been saved.');
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to update your address.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="panel loading-screen"><div><div className="spinner" /><p>Loading your profile…</p></div></div>;
  }
  if (!profile) {
    return <section className="panel"><div className="notice notice-error" role="alert">{error || 'Your profile could not be loaded.'}</div><button className="button button-secondary" onClick={onClose}>Back to dashboard</button></section>;
  }

  const readOnlyDetails = [
    ['Employee ID', profile.employeeId],
    ['Legal name', profile.legalName],
    ['Department', profile.department],
    ['Designation', profile.designation],
    ['Joining date', profile.joiningDate],
    ['Manager', profile.managerName || 'Not assigned'],
  ];

  return (
    <>
      <div className="page-heading">
        <div><h1>My profile</h1><p>Your employment details are read-only. You can update your address below.</p></div>
        <button className="button button-secondary" onClick={onClose}>Back to dashboard</button>
      </div>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {success && <div className="notice notice-success" role="status">{success}</div>}
      <section className="panel">
        <h2 className="panel-title">Employment information <small>Managed by your organization</small></h2>
        <div className="profile-grid">
          {readOnlyDetails.map(([label, value]) => (
            <div className="profile-item" key={label}>
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </section>
      <section className="panel">
        <div className="page-heading">
          <div><h2 className="panel-title">Contact address</h2><p>Keep both address records current.</p></div>
          {!editing && <button className="button button-secondary" onClick={beginEditing}>Edit address</button>}
        </div>
        {editing ? (
          <form onSubmit={save}>
            <div className="address-columns">
              {(['Residential address', 'Mailing address'] as const).map((group, groupIndex) => (
                <fieldset className="address-fieldset" key={group}>
                  <legend>{group}</legend>
                  <div className="form-grid">
                    {addressFields.slice(groupIndex * 5, groupIndex * 5 + 5).map(({ key, label, maxLength }) => (
                      <label className="field" key={key} htmlFor={key}>
                        {label}
                        <input
                          id={key}
                          value={form[key]}
                          onChange={(event) => updateField(key, event.target.value)}
                          maxLength={maxLength}
                          required
                          disabled={saving}
                        />
                      </label>
                    ))}
                  </div>
                  {groupIndex === 0 && (
                    <button className="text-button" type="button" onClick={copyResidential}>
                      Copy residential address to mailing
                    </button>
                  )}
                </fieldset>
              ))}
            </div>
            <div className="form-actions">
              <button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save address'}</button>
              <button className="button button-secondary" type="button" onClick={cancelEditing} disabled={saving}>Cancel</button>
            </div>
          </form>
        ) : profile.address ? (
          <div className="address-columns">
            {[
              ['Residential address', profile.address.residentialStreet, profile.address.residentialCity, profile.address.residentialState, profile.address.residentialPostalCode, profile.address.residentialCountry],
              ['Mailing address', profile.address.mailingStreet, profile.address.mailingCity, profile.address.mailingState, profile.address.mailingPostalCode, profile.address.mailingCountry],
            ].map(([label, ...lines]) => (
              <div className="address-display" key={label}>
                <h3>{label}</h3>
                {lines.map((line, index) => <span key={index}>{line}</span>)}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">No address has been added yet. Choose “Edit address” to add one.</div>
        )}
      </section>
    </>
  );
};
