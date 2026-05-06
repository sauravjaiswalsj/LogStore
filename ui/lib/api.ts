import {
  ActivityEvent,
  ClusterOverview,
  HealthSnapshot,
  TabletDetail,
  TabletSummary
} from "@/lib/types";
import { API_BASE_URL } from "@/lib/backend";
import { touchBackendHealthKeepalive } from "@/lib/health-keepalive";

async function fetchJson<T>(path: string): Promise<T | null> {
  touchBackendHealthKeepalive();

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      cache: "no-store"
    });

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export async function getHealthSnapshot(): Promise<HealthSnapshot | null> {
  return fetchJson<HealthSnapshot>("/health");
}

export async function getTabletSummaries(): Promise<TabletSummary[]> {
  return (await fetchJson<TabletSummary[]>("/tablets")) ?? [];
}

export async function getTabletDetail(
  tabletId: number,
  recentLimit = 12
): Promise<TabletDetail | null> {
  return fetchJson<TabletDetail>(
    `/tablets/${tabletId}?recentLimit=${encodeURIComponent(String(recentLimit))}`
  );
}

export async function getClusterOverview(): Promise<ClusterOverview | null> {
  return fetchJson<ClusterOverview>("/cluster");
}

export async function getRecentActivity(limit = 8): Promise<ActivityEvent[]> {
  const tablets = await getTabletSummaries();
  const activeTablets = tablets
    .filter((tablet) => tablet.latestOffset >= 0)
    .slice(0, 10);

  const details = await Promise.all(
    activeTablets.map((tablet) => getTabletDetail(tablet.tabletId, 4))
  );

  return details
    .flatMap((detail) =>
      (detail?.recentRecords ?? []).map((record) => ({
        tabletId: detail?.tabletId ?? -1,
        offset: record.offset,
        timestamp: record.timestamp,
        key: record.key,
        value: record.value
      }))
    )
    .sort((left, right) => right.timestamp - left.timestamp)
    .slice(0, limit);
}
