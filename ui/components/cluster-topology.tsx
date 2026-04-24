import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { ClusterOverview } from "@/lib/types";

export function ClusterTopology({
  cluster,
  totalTablets
}: {
  cluster: ClusterOverview | null;
  totalTablets: number;
}) {
  return (
    <div className="grid grid--2">
      <Panel
        title="Topology posture"
        description="The visual language is ready for cluster mode, but the backend is still exposing a single-node shape."
        action={<StatusPill status={cluster?.status ?? "pending"} />}
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">{cluster?.topologyMode ?? "backend pending"}</strong>
            <p>Current tablets: {totalTablets}</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">Leader election: {cluster?.leaderElection ?? "pending"}</strong>
            <p>Election transitions will land here once node roles and votes are surfaced by the Java service.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">Replication: {cluster?.replication ?? "pending"}</strong>
            <p>Follower lag, quorum health, and leader ownership remain explicitly marked as pending.</p>
          </div>
        </div>
      </Panel>

      <Panel
        title="Intentional placeholders"
        description="These are not fake metrics. They mark where the next backend integrations belong."
      >
        <div className="callout">
          {cluster?.note ??
            "Cluster metadata is not yet live. The interface reserves space for leader/follower topology, election state, and lag once the service exposes those facts."}
        </div>
      </Panel>
    </div>
  );
}
