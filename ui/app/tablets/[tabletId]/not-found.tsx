import Link from "next/link";

import { PageHeader } from "@/components/page-header";
import { Panel } from "@/components/panel";

export default function TabletNotFound() {
  return (
    <div className="grid">
      <PageHeader
        description="The requested tablet does not exist or the backend could not return detail for it."
        eyebrow="tablet / missing"
        title="Tablet not found"
      />
      <Panel title="Next step" description="Return to the tablet inventory and pick a valid partition.">
        <Link className="button button--ghost" href="/tablets">
          Back to tablets
        </Link>
      </Panel>
    </div>
  );
}
