import React, { useEffect, useState } from 'react';
import { payslipService } from '../services/payslipService';
import { Payslip } from '../types/payslip';

const dateLabel = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString();

export const PayslipPage: React.FC = () => {
  const [payslips, setPayslips] = useState<Payslip[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setLoadError(null);
    payslipService.getPayslips(page)
      .then((result) => {
        if (!active) return;
        setPayslips(result.content);
        setTotalPages(result.totalPages);
      })
      .catch((reason: unknown) => {
        if (active) setLoadError(reason instanceof Error ? reason.message : 'Unable to load your payslips.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, refreshKey]);

  const download = async (payslip: Payslip) => {
    setDownloadError(null);
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
      setDownloadError(reason instanceof Error ? reason.message : 'Unable to download this payslip.');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <>
      <div className="page-heading">
        <div><h1>My payslips</h1><p>Access your confidential pay statements.</p></div>
      </div>
      {loadError && <div className="notice notice-error" role="alert">{loadError}</div>}
      {downloadError && <div className="notice notice-error" role="alert">{downloadError}</div>}
      <section className="panel">
        <h2 className="panel-title">Pay periods</h2>
        {loading ? (
          <div className="loading-screen"><div><div className="spinner" /><p>Loading your payslips…</p></div></div>
        ) : loadError ? (
          <div className="empty-state">
            Payslips could not be loaded.
            <div className="form-actions">
              <button className="button button-secondary" onClick={() => setRefreshKey((value) => value + 1)}>Try again</button>
            </div>
          </div>
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
        {!loading && !loadError && totalPages > 1 && <div className="pagination-row">
          <button className="button button-secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous payslips</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button className="button button-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>More payslips</button>
        </div>}
      </section>
    </>
  );
};
