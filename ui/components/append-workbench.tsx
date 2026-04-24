"use client";

import { useMemo, useState } from "react";

import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { formatDateTime } from "@/lib/format";
import { routeTabletForKey } from "@/lib/java-hash";
import { AppendResult } from "@/lib/types";

type TrailEvent = AppendResult & {
  key: string;
  value: string;
  createdAt: string;
};

export function AppendWorkbench({ totalTablets }: { totalTablets: number }) {
  const [key, setKey] = useState("");
  const [value, setValue] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState<TrailEvent | null>(null);
  const [trail, setTrail] = useState<TrailEvent[]>([]);
  const [error, setError] = useState<string | null>(null);

  const predictedTablet = useMemo(
    () => routeTabletForKey(key, totalTablets),
    [key, totalTablets]
  );

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await fetch("/api/append", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ key, value })
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.error ?? "Append failed");
      }

      const nextReceipt: TrailEvent = {
        ...(payload as AppendResult),
        key,
        value,
        createdAt: new Date().toISOString()
      };

      setReceipt(nextReceipt);
      setTrail((current) => [nextReceipt, ...current].slice(0, 8));
      setValue("");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Append failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid grid--2">
      <Panel
        title="Producer"
        description="Write a record, predict its route, and confirm where the backend actually stored it."
        action={<StatusPill status="interactive" />}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="key">Key</label>
            <input
              id="key"
              name="key"
              onChange={(event) => setKey(event.target.value)}
              placeholder="user-123"
              required
              value={key}
            />
          </div>
          <div className="field">
            <label htmlFor="value">Value</label>
            <textarea
              id="value"
              name="value"
              onChange={(event) => setValue(event.target.value)}
              placeholder="CREATE_TASK"
              required
              value={value}
            />
          </div>

          <div className="split-line mono">
            <span>Predicted route</span>
            <strong>tablet-{predictedTablet}</strong>
          </div>

          {error ? <div className="error-text">{error}</div> : null}

          <div className="button-row">
            <button className="button" disabled={submitting} type="submit">
              {submitting ? "Appending..." : "Append record"}
            </button>
            <button
              className="button button--ghost"
              onClick={() => {
                setKey("");
                setValue("");
                setError(null);
              }}
              type="button"
            >
              Reset
            </button>
          </div>
        </form>
      </Panel>

      <div className="grid">
        <Panel
          title="Latest receipt"
          description="This panel confirms the backend response immediately after append."
          action={receipt ? <StatusPill status="confirmed" /> : <StatusPill status="waiting" />}
        >
          {receipt ? (
            <div className="receipt">
              <div className="split-line mono">
                <span>Routed tablet</span>
                <strong>tablet-{receipt.tabletId}</strong>
              </div>
              <div className="split-line mono">
                <span>Assigned offset</span>
                <strong>{receipt.offset}</strong>
              </div>
              <div className="split-line">
                <span className="muted">Recorded</span>
                <span>{formatDateTime(receipt.createdAt)}</span>
              </div>
              <div className="value-block">{receipt.value}</div>
            </div>
          ) : (
            <div className="empty-state">
              Submit a record to see the backend receipt and offset assignment.
            </div>
          )}
        </Panel>

        <Panel
          title="Append trail"
          description="Session-local timeline so you can keep writing without losing context."
        >
          {trail.length > 0 ? (
            <div className="timeline">
              {trail.map((item, index) => (
                <div className="timeline-item" key={`${item.tabletId}-${item.offset}-${index}`}>
                  <div className="split-line mono">
                    <strong>
                      tablet-{item.tabletId} / offset-{item.offset}
                    </strong>
                    <span>{formatDateTime(item.createdAt)}</span>
                  </div>
                  <p>
                    key={item.key} value={item.value.slice(0, 90)}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              Your append trail will fill in here as soon as you write records.
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
}
