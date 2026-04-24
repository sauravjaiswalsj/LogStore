import Link from "next/link";

import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { TabletMatrix } from "@/components/tablet-matrix";
import { getTabletSummaries } from "@/lib/api";
import {
  formatBytes,
  formatDateTime,
  formatNumber
} from "@/lib/format";

export const dynamic = "force-dynamic";

export default async function TabletsPage() {
  const tablets = await getTabletSummaries();

  return (
    <div className="grid">
      <PageHeader
        aside={<StatusPill status={`${tablets.length} tablets`} />}
        description="Read the full partition surface as a matrix first, then use the dense table for precision work."
        eyebrow="tablets / partition map"
        title="Every tablet is first-class"
      />

      <Panel
        title="Tablet heatmap"
        description="High-signal cards optimized for scanning before drilling into a specific log."
      >
        <TabletMatrix tablets={tablets} />
      </Panel>

      <Panel
        title="Tablet inventory"
        description="Detailed metadata for every partition, including size, latest offset, and direct read access."
      >
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Tablet</th>
                <th>Status</th>
                <th>Latest offset</th>
                <th>Records</th>
                <th>File size</th>
                <th>Last modified</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {tablets.map((tablet) => (
                <tr key={tablet.tabletId}>
                  <td className="mono">tablet-{tablet.tabletId}</td>
                  <td>
                    <StatusPill status={tablet.status} />
                  </td>
                  <td className="mono">
                    {tablet.latestOffset >= 0 ? tablet.latestOffset : "empty"}
                  </td>
                  <td className="mono">{formatNumber(tablet.recordCount)}</td>
                  <td className="mono">{formatBytes(tablet.fileSizeBytes)}</td>
                  <td>{formatDateTime(tablet.lastModifiedAt)}</td>
                  <td>
                    <Link className="button button--ghost" href={`/tablets/${tablet.tabletId}`}>
                      Inspect
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}
