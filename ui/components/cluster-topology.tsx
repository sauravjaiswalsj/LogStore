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
        description="Static V0.3 cluster state from the configured leader and followers."
        action={<StatusPill status={cluster?.status ?? "pending"} />}
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">{cluster?.nodeId ?? "backend pending"}</strong>
            <p>{cluster?.leader ? "Leader" : "Follower"} · {cluster?.topologyMode ?? "pending"} · tablets: {totalTablets}</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">Ack mode: {cluster?.ackMode ?? "pending"}</strong>
            <p>Replication factor: {cluster?.replicationFactor ?? 1} · commit offset: {cluster?.commitOffset ?? -1}</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">Latest offset: {cluster?.latestOffset ?? -1}</strong>
            <p>Leader election: {cluster?.leaderElection ?? "manual static leader"}</p>
          </div>
        </div>
      </Panel>

      <Panel
        title="Follower lag"
        description="Peer health is sampled from each follower's cluster status endpoint."
      >
        {cluster?.peers?.length ? (
          <div className="timeline">
            {cluster.peers.map((peer) => (
              <div className="timeline-item" key={peer.peer}>
                <strong className="mono">{peer.peer}</strong>
                <p>{peer.healthy ? "healthy" : "unreachable"} · latest offset: {peer.latestOffset} · lag: {peer.lag}</p>
              </div>
            ))}
          </div>
        ) : (
          <div className="callout">{cluster?.note ?? "No follower peers configured."}</div>
        )}
      </Panel>
    </div>
  );
}
