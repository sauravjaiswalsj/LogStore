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
        description="Static leader/follower replication state for the V0.3 alpha."
        eyebrow="cluster / topology"
        title="Cluster topology"
      />

      <ClusterTopology cluster={cluster} totalTablets={health?.totalTablets ?? 0} />

      <Panel
        title="V0.3 boundary"
        description="The alpha demonstrates replication without automatic leader election."
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">leader</strong>
            <p>{cluster?.nodeId ?? "node-1"} is configured statically.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">quorum</strong>
            <p>Leader plus one follower is enough when replication factor is three.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">failover</strong>
            <p>Leader failover remains manual and unsupported in this alpha.</p>
          </div>
        </div>
      </Panel>
    </div>
  );
}
