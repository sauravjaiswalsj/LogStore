import { AppendWorkbench } from "@/components/append-workbench";
import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";
import { StatusPill } from "@/components/status-pill";
import { getHealthSnapshot } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function AppendPage() {
  const health = await getHealthSnapshot();
  const totalTablets = health?.totalTablets ?? 10;

  return (
    <div className="grid">
      <PageHeader
        aside={<StatusPill status="live writes" />}
        description="Produce records into the system and verify tablet routing and offset assignment with no extra ceremony."
        eyebrow="append / producer workbench"
        title="Write, route, confirm"
      />

      <AppendWorkbench totalTablets={totalTablets} />

      <Panel
        title="Why this page matters"
        description="The append path is the heart of the product, so the interface keeps cause and effect close together."
      >
        <div className="timeline">
          <div className="timeline-item">
            <strong className="mono">1. predict route</strong>
            <p>The client mirrors Java hash routing so you get an immediate expectation before the write leaves the browser.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">2. append record</strong>
            <p>The request goes through a local Next.js proxy route, then reaches the Spring Boot service unchanged.</p>
          </div>
          <div className="timeline-item">
            <strong className="mono">3. inspect receipt</strong>
            <p>The backend response becomes the confirmation artifact: tablet, offset, timestamp, and append trail.</p>
          </div>
        </div>
      </Panel>
    </div>
  );
}
