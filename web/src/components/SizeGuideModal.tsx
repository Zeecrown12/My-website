import { useState } from 'react';
import { Ruler, X } from 'lucide-react';
import { SizeGuide } from '../types/store';

export function SizeGuideModal({
  guide,
  onClose,
}: {
  guide: SizeGuide;
  onClose: () => void;
}) {
  const [unit, setUnit] = useState<'CM' | 'IN'>(guide.measurementUnitDefault || 'CM');

  return (
    <div className="zc-modal-backdrop" role="dialog" aria-modal="true" aria-label={guide.name}>
      <div className="zc-modal">
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
            borderBottom: '1px solid #D9D9E0',
            paddingBottom: 12,
            marginBottom: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Ruler size={20} color="#542080" />
            <h3 style={{ margin: 0 }}>{guide.name}</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close size guide"
            className="zc-pill-btn"
          >
            <X size={16} />
          </button>
        </div>

        {guide.description && (
          <p style={{ color: '#5C4B6E', marginTop: 0 }}>{guide.description}</p>
        )}

        <div className="chip-row" style={{ marginBottom: 14 }}>
          <button
            type="button"
            className={`chip ${unit === 'CM' ? 'active' : ''}`}
            onClick={() => setUnit('CM')}
          >
            Centimetres (CM)
          </button>
          <button
            type="button"
            className={`chip ${unit === 'IN' ? 'active' : ''}`}
            onClick={() => setUnit('IN')}
          >
            Inches (IN)
          </button>
        </div>

        {guide.columns.length > 0 && guide.rows.length > 0 && (
          <div className="table-wrap" style={{ marginBottom: 18 }}>
            <table className="zc-table">
              <thead>
                <tr>
                  {guide.columns.map((col) => (
                    <th key={col}>{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {guide.rows.map((row, idx) => (
                  <tr key={idx}>
                    {guide.columns.map((col) => {
                      const raw = String(row[col] ?? row[col.toLowerCase()] ?? '—');
                      const numeric = Number(raw);
                      const converted =
                        !Number.isNaN(numeric) &&
                        raw.trim() !== '' &&
                        col.toLowerCase() !== 'size' &&
                        col.toLowerCase() !== 'eu' &&
                        col.toLowerCase() !== 'uk' &&
                        col.toLowerCase() !== 'us'
                          ? unit === 'IN' && guide.measurementUnitDefault !== 'IN'
                            ? `${(numeric / 2.54).toFixed(1)}"`
                            : unit === 'CM' && guide.measurementUnitDefault === 'IN'
                            ? `${(numeric * 2.54).toFixed(1)} cm`
                            : raw
                          : raw;
                      return <td key={col}>{converted}</td>;
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {guide.howToMeasureSteps && guide.howToMeasureSteps.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <h4 style={{ marginBottom: 8 }}>How to Measure</h4>
            <ol style={{ paddingLeft: 20, margin: 0, color: '#1F122B' }}>
              {guide.howToMeasureSteps.map((step, i) => (
                <li key={i} style={{ marginBottom: 6 }}>
                  {step}
                </li>
              ))}
            </ol>
          </div>
        )}

        {guide.fitNotes && (
          <div className="alert alert-info" style={{ marginBottom: 0 }}>
            <strong>Boutique Fit Guidance:</strong> {guide.fitNotes}
          </div>
        )}
      </div>
    </div>
  );
}
