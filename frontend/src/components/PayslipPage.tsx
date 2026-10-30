import React, { useEffect, useState } from 'react';
import { payslipService } from '../services/payslipService';
import { Payslip } from '../types/payslip';

const dateLabel = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString();

export const PayslipPage: React.FC = () => {
  const [payslips, setPayslips] = useState<Payslip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    payslipService.getPayslips()
      .then((items) => { if (active) setPayslips(items); })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : 'Unable to load your payslips.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const loadPayslips = async () => {
    setLoading(true);
    setError(null);
    try {
      setPayslips(await payslipService.getPayslips());
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to load your payslips.');
    } finally {
      setLoading(false);
    }
  };

  const download = async (payslip: Payslip) => {
    setError(null);
    setDownloadingId(payslip.id);
    try {
      const file = await payslipService.download(payslip.id);
      const url = URL.createObjectURL(file);
      const link = document.createElement('a');
      link.href = url;
      link.download = `payslip-${payslip.payPeriodStart.slice(0, 7)}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : 'Unable to download this payslip.');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <>
      <div className="page-heading">
        <div><h1>My payslips</h1><p>Access your confidential pay statements.</p></div>
      </div>
      {error && <div className="notice notice-error" role="alert">{error}</div>}
      <section className="panel">
        <h2 className="panel-title">Pay periods</h2>
        {loading ? (
          <div className="loading-screen"><div><div className="spinner" /><p>Loading your payslips…</p></div></div>
        ) : payslips.length === 0 ? (
          <div className="empty-state">No payslips are available for your account.</div>
        ) : (
          <div className="data-list">
            {payslips.map((payslip) => (
              <div className="data-row" key={payslip.id}>
                <div>
                  <strong>{dateLabel(payslip.payPeriodStart)} – {dateLabel(payslip.payPeriodEnd)}</strong>
                  <br /><span>Issued {dateLabel(payslip.issuedAt.slice(0, 10))}</span>
                </div>
                <button
                  className="button button-secondary"
                  onClick={() => void download(payslip)}
                  disabled={downloadingId !== null}
                >
                  {downloadingId === payslip.id ? 'Downloading…' : 'Download PDF'}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  );
};
