import Link from "next/link";

import { StatusPill } from "@/components/status-pill";
import { formatBytes, formatNumber, formatRelativeTime } from "@/lib/format";
import { TabletSummary } from "@/lib/types";

export function TabletMatrix({ tablets }: { tablets: TabletSummary[] }) {
  return (
    <div className="tablet-matrix">
      {tablets.map((tablet) => (
        <Link
          key={tablet.tabletId}
          className="tablet-card"
          href={`/tablets/${tablet.tabletId}`}
        >
          <div className="tablet-card__top">
            <div>
              <div className="tablet-card__label mono">tablet-{tablet.tabletId}</div>
              <div className="tablet-card__meta">
                last change {formatRelativeTime(tablet.lastModifiedAt)}
              </div>
            </div>
            <StatusPill status={tablet.status} />
          </div>

          <div className="split-line mono">
            <span>latest offset</span>
            <strong>{tablet.latestOffset >= 0 ? tablet.latestOffset : "empty"}</strong>
          </div>
          <div className="split-line mono">
            <span>records</span>
            <strong>{formatNumber(tablet.recordCount)}</strong>
          </div>
          <div className="split-line mono">
            <span>footprint</span>
            <strong>{formatBytes(tablet.fileSizeBytes)}</strong>
          </div>
        </Link>
      ))}
    </div>
  );
}
