import { ClusterTopology } from "@/components/cluster-topology";
import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { getClusterOverview, getHealthSnapshot } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function ClusterPage() {
  const [cluster, health] = await Promise.all([
    getClusterOverview(),
    getHealthSnapshot()
  ]);

  return (
    <div className="grid">
      <PageHeader
        aside={<StatusPill status={cluster?.status ?? "pending"} />}
        description="The UI reserves space for leadership, replication, and election state without pretending those signals exist before the backend does."
        eyebrow="cluster / topology"
        title="Topology with honest gaps"
      />

      <ClusterTopology cluster={cluster} totalTablets={health?.totalTablets ?? 0} />

      <Panel
        title="Planned next interfaces"
        description="The frontend is ready to absorb richer cluster state as soon as the service exposes it."
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">leader assignments per tablet</strong>
            <p>Show ownership, failover, and replica followers for every partition.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">replication lag and ack state</strong>
            <p>Render lag heatmaps and warnings rather than static placeholder badges.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">election state transitions</strong>
            <p>Expose candidate, follower, and leader movement in a time-based event stream.</p>
          </div>
        </div>
      </Panel>
    </div>
  );
}
