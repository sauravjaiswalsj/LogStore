import Link from "next/link";
import { notFound } from "next/navigation";

import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";
import { ReadConsole } from "@/components/read-console";
import { StatusPill } from "@/components/status-pill";
import { getTabletDetail } from "@/lib/api";
import { formatBytes, formatDateTime, formatNumber } from "@/lib/format";

export const dynamic = "force-dynamic";

export default async function TabletDetailPage({
  params
}: {
  params: Promise<{ tabletId: string }>;
}) {
  const { tabletId } = await params;
  const detail = await getTabletDetail(Number(tabletId), 12);

  if (!detail) {
    notFound();
  }

  return (
    <div className="grid">
      <PageHeader
        aside={<StatusPill status={detail.status} />}
        description="Inspect one partition at a time with offset controls, recent history, and honest metadata straight from the backend."
        eyebrow={`tablet / tablet-${detail.tabletId}`}
        title={`Tablet ${detail.tabletId}`}
      />

      <div className="grid grid--4">
        <Panel title="Latest offset" description="Current visible tail for this tablet.">
          <div className="stat-card__value mono">
            {detail.latestOffset >= 0 ? detail.latestOffset : "empty"}
          </div>
        </Panel>
        <Panel title="Record count" description="Recovered from the tablet's monotonic next offset.">
          <div className="stat-card__value mono">{formatNumber(detail.recordCount)}</div>
        </Panel>
        <Panel title="File size" description="On-disk footprint of the append-only log file.">
          <div className="stat-card__value mono">{formatBytes(detail.fileSizeBytes)}</div>
        </Panel>
        <Panel
          title="Log path"
          description="Useful for local inspection while debugging the Java service."
        >
          <div className="value-block mono">{detail.logFilePath}</div>
        </Panel>
      </div>

      <ReadConsole detail={detail} />

      <Panel
        title="Tablet metadata"
        description="A few durable facts that help while debugging or replaying issues."
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">last modified</strong>
            <p>{formatDateTime(detail.lastModifiedAt)}</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">file present</strong>
            <p>{detail.logFileExists ? "Yes" : "No"}</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">navigation</strong>
            <p>
              <Link href="/tablets">Back to tablet inventory</Link>
            </p>
          </div>
        </div>
      </Panel>
    </div>
  );
}
