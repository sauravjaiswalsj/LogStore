import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";
import { RequestsPanel } from "@/components/requests-panel";
import { StatCard } from "@/components/stat-card";
import { StatusPill } from "@/components/status-pill";
import { TabletMatrix } from "@/components/tablet-matrix";
import {
  getClusterOverview,
  getHealthSnapshot,
  getRecentActivity,
  getTabletSummaries
} from "@/lib/api";
import { formatDateTime, formatNumber } from "@/lib/format";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const [health, tablets, cluster, recentActivity] = await Promise.all([
    getHealthSnapshot(),
    getTabletSummaries(),
    getClusterOverview(),
    getRecentActivity()
  ]);

  const activeTablets = tablets.filter((tablet) => tablet.latestOffset >= 0);
  const unhealthyCount = tablets.filter((tablet) => tablet.status !== "active").length;
  const maxOffset =
    tablets.reduce((max, tablet) => Math.max(max, tablet.latestOffset), -1) ?? -1;

  return (
    <div className="grid">
      <PageHeader
        aside={<StatusPill status={health?.status ?? "offline"} />}
        description="A dense operator surface for watching append-only traffic move through tablets, offsets, and future cluster controls."
        eyebrow="overview / cluster snapshot"
        title="See the system working"
      />

      <div className="grid grid--4">
        <StatCard
          badge={<StatusPill status={health?.status ?? "unknown"} />}
          description="Current app health from the backend snapshot."
          title="Service status"
          value={health?.status ?? "DOWN"}
        />
        <StatCard
          description="Total tablets configured by the Java service."
          title="Configured tablets"
          value={formatNumber(health?.totalTablets ?? tablets.length)}
        />
        <StatCard
          description="Tablets with live log files and readable history."
          title="Active tablets"
          value={formatNumber(activeTablets.length)}
        />
        <StatCard
          badge={<StatusPill status={cluster?.status ?? "pending"} />}
          description="Highest offset currently visible anywhere in the system."
          title="Observed tail"
          value={maxOffset >= 0 ? formatNumber(maxOffset) : "empty"}
        />
      </div>

      <div className="grid grid--3">
        <Panel
          title="Tablet matrix"
          description="Scan the entire partition surface first, then drill into a tablet when something looks interesting."
        >
          <TabletMatrix tablets={tablets} />
        </Panel>

        <Panel
          title="Recent append activity"
          description="Derived from the most recent records visible across tablet detail endpoints."
        >
          {recentActivity.length > 0 ? (
            <div className="timeline">
              {recentActivity.map((event) => (
                <div
                  className="timeline-item"
                  key={`${event.tabletId}-${event.offset}-${event.timestamp}`}
                >
                  <div className="split-line mono">
                    <strong>
                      tablet-{event.tabletId} / offset-{event.offset}
                    </strong>
                    <span>{formatDateTime(event.timestamp)}</span>
                  </div>
                  <p>
                    key={event.key} value={event.value.slice(0, 90)}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              No append history is visible yet. Write a few records in the append workbench and this panel will come alive.
            </div>
          )}
        </Panel>

        <Panel
          title="Operational notes"
          description="What the current backend can and cannot prove yet."
        >
          <div className="timeline">
            <div className="timeline-item">
              <strong className="mono">mode: {health?.mode ?? "backend unavailable"}</strong>
              <p>The UI assumes a developer/operator audience and keeps system state front and center.</p>
            </div>
            <div className="timeline-item">
              <strong className="mono">non-active tablets: {unhealthyCount}</strong>
              <p>Idle and empty tablets are shown explicitly instead of being hidden behind success-colored charts.</p>
            </div>
            <div className="timeline-item">
              <strong className="mono">
                cluster note: {cluster?.leaderElection ?? "pending"}
              </strong>
              <p>Replication, leaders, and election visuals remain honest placeholders until those facts exist in the API.</p>
            </div>
          </div>
        </Panel>
      </div>

      <div className="grid grid--2">
        <RequestsPanel />

        <Panel
          title="Data freshness"
          description="This page uses no-store fetches so operators see live state rather than stale build-time snapshots."
        >
          <div className="timeline">
            <div className="timeline-item">
              <strong className="mono">health observed</strong>
              <p>{health?.timestamp ? formatDateTime(health.timestamp) : "Unavailable"}</p>
            </div>
            <div className="timeline-item">
              <strong className="mono">available log files</strong>
              <p>{formatNumber(health?.availableLogs ?? 0)}</p>
            </div>
          </div>
        </Panel>
      </div>
    </div>
  );
}
