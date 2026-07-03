import { ReactNode } from 'react';
import { QualityBadge } from '../QualityBadge';
import type { QualityLevel } from '../../tokens/colors';
import { Icon } from '../Icon';

export interface HistoryTableRow {
  dateLabel: string;
  downloadLabel: string;
  id: string;
  latencyLabel: string;
  qualityLabel: string;
  qualityLevel: QualityLevel;
  stabilityLabel: string;
}

export interface HistoryTableProps {
  onOpen: (id: string) => void;
  rows: HistoryTableRow[];
  trailing?: (row: HistoryTableRow) => ReactNode;
}

export function HistoryTable({ onOpen, rows, trailing }: HistoryTableProps) {
  return (
    <div className="sq-history-table">
      <div className="sq-history-table__head">
        <span className="label-small">Data e hora</span>
        <span className="label-small">Status</span>
        <span className="label-small">Download</span>
        <span className="label-small">Ping</span>
        <span className="label-small">Estabilidade</span>
        <span />
      </div>
      {rows.map((row) => (
        <button className="sq-history-table__row" key={row.id} onClick={() => onOpen(row.id)} type="button">
          <span className="sq-history-table__date">{row.dateLabel}</span>
          <QualityBadge label={row.qualityLabel} level={row.qualityLevel} />
          <span className="sq-history-table__download">{row.downloadLabel}</span>
          <span className="sq-history-table__cell">{row.latencyLabel}</span>
          <span className={`sq-history-table__cell sq-history-table__cell--${row.qualityLevel}`}>{row.stabilityLabel}</span>
          {trailing ? trailing(row) : <Icon name="chevron_right" size={20} />}
        </button>
      ))}

      <div className="sq-history-cards">
        {rows.map((row) => (
          <button className="sq-history-card" key={row.id} onClick={() => onOpen(row.id)} type="button">
            <div className="sq-history-card__top">
              <span className="sq-history-card__date">{row.dateLabel}</span>
              <QualityBadge label={row.qualityLabel} level={row.qualityLevel} />
            </div>
            <div className="sq-history-card__bottom">
              <span className="sq-history-card__download">{row.downloadLabel}</span>
              <span className="body-small">{row.latencyLabel}</span>
              <span className={`body-small sq-history-table__cell--${row.qualityLevel}`}>{row.stabilityLabel}</span>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
