import { ReactNode } from "react";

import { Panel } from "@/components/panel";

export function StatCard({
  title,
  description,
  value,
  badge
}: {
  title: string;
  description: string;
  value: string;
  badge?: ReactNode;
}) {
  return (
    <Panel title={title} description={description} action={badge}>
      <div className="stat-card__value mono">{value}</div>
    </Panel>
  );
}
