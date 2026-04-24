"use client";

import { useState } from "react";

import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { formatDateTime } from "@/lib/format";
import { LogRecord, TabletDetail } from "@/lib/types";

export function ReadConsole({ detail }: { detail: TabletDetail }) {
  const [startOffset, setStartOffset] = useState(
    detail.latestOffset >= 0 ? Math.max(0, detail.latestOffset - 9) : 0
  );
  const [limit, setLimit] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [records, setRecords] = useState<LogRecord[]>(detail.recentRecords);

  async function handleRead(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams({
        tabletId: String(detail.tabletId),
        startOffset: String(startOffset),
        limit: String(limit)
      });
      const response = await fetch(`/api/read?${params.toString()}`);
      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.error ?? "Read failed");
      }

      setRecords(payload.logRecords ?? []);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Read failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid grid--2">
      <Panel
        title="Offset navigator"
        description="Jump to any readable range and inspect records without leaving the tablet view."
        action={<StatusPill status="readable" />}
      >
        <form className="form-grid" onSubmit={handleRead}>
          <div className="field">
            <label htmlFor="startOffset">Start offset</label>
            <input
              id="startOffset"
              min={0}
              onChange={(event) => setStartOffset(Number(event.target.value))}
              type="number"
              value={startOffset}
            />
          </div>
          <div className="field">
            <label htmlFor="limit">Limit</label>
            <select
              id="limit"
              onChange={(event) => setLimit(Number(event.target.value))}
              value={limit}
            >
              {[5, 10, 20, 50].map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>

          <div className="button-row">
            <button className="button" disabled={loading} type="submit">
              {loading ? "Reading..." : "Read records"}
            </button>
            <button
              className="button button--ghost"
              onClick={() =>
                setStartOffset(detail.latestOffset >= 0 ? Math.max(0, detail.latestOffset - 9) : 0)
              }
              type="button"
            >
              Jump near tail
            </button>
          </div>
        </form>
        {error ? <div className="error-text">{error}</div> : null}
      </Panel>

      <Panel
        title="Query notes"
        description="What this tablet can actually support right now."
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">latest offset: {detail.latestOffset}</strong>
            <p>Reads are offset-based and limited; results are immutable log records.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">next offset: {detail.nextOffset}</strong>
            <p>Offsets are monotonic. When new writes arrive, the tail advances without reshuffling old records.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">backend shape: GET /read</strong>
            <p>The current implementation reads from a specific tablet and starting offset, with an optional limit.</p>
          </div>
        </div>
      </Panel>

      <Panel
        title="Record stream"
        description="Sticky headers and a dense table keep the log readable under operator load."
      >
        {records.length > 0 ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Offset</th>
                  <th>Timestamp</th>
                  <th>Key</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                {records.map((record) => (
                  <tr key={`${record.offset}-${record.timestamp}`}>
                    <td className="mono">{record.offset}</td>
                    <td>{formatDateTime(record.timestamp)}</td>
                    <td className="mono">{record.key}</td>
                    <td>
                      <div className="value-block">{record.value}</div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            No records in this range yet. Try a lower offset or append data first.
          </div>
        )}
      </Panel>
    </div>
  );
}
