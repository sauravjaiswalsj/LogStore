"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode } from "react";

const NAV_ITEMS = [
  {
    href: "/",
    title: "Overview",
    description: "Cluster snapshot and recent append flow"
  },
  {
    href: "/append",
    title: "Append",
    description: "Produce records and watch routing in real time"
  },
  {
    href: "/tablets",
    title: "Tablets",
    description: "Inspect partitions, offsets, and log files"
  },
  {
    href: "/cluster",
    title: "Cluster",
    description: "Topology, leadership, and replication posture"
  }
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <div className="eyebrow">mission control / logstore</div>
          <h1 className="brand-title">Distributed Log Console</h1>
          <p className="brand-copy">
            Watch keys route into tablets, inspect offsets, and keep the system
            legible while the backend grows into full cluster mode.
          </p>
        </div>

        <nav className="nav-list">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.href}
              className="nav-link"
              data-active={pathname === item.href}
              href={item.href}
            >
              <strong>{item.title}</strong>
              <span>{item.description}</span>
            </Link>
          ))}
        </nav>

        <div className="sidebar__footer">
          <div className="eyebrow">runtime</div>
          <p className="brand-copy">
            Frontend proxies backend requests through local Next.js route
            handlers, so the UI can evolve without adding browser CORS work.
          </p>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}
