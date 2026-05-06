import { API_BASE_URL } from "@/lib/backend";

type KeepaliveState = {
  inFlight: boolean;
  lastAttemptAt: number;
  timer: ReturnType<typeof setInterval> | null;
};

declare global {
  var __logstoreHealthKeepalive: KeepaliveState | undefined;
}

const HEALTH_INTERVAL_MS = Number(
  process.env.LOGSTORE_HEALTH_CHECK_INTERVAL_MS ?? 10 * 60 * 1000
);

function state(): KeepaliveState {
  globalThis.__logstoreHealthKeepalive ??= {
    inFlight: false,
    lastAttemptAt: 0,
    timer: null
  };
  return globalThis.__logstoreHealthKeepalive;
}

async function pingHealthIfDue() {
  const keepalive = state();
  const now = Date.now();

  if (
    keepalive.inFlight ||
    now - keepalive.lastAttemptAt < HEALTH_INTERVAL_MS
  ) {
    return;
  }

  keepalive.inFlight = true;
  keepalive.lastAttemptAt = now;

  try {
    await fetch(`${API_BASE_URL}/health`, {
      cache: "no-store"
    });
  } catch {
    // Keepalive is best-effort; normal API requests still surface failures.
  } finally {
    keepalive.inFlight = false;
  }
}

export function touchBackendHealthKeepalive() {
  const keepalive = state();

  if (!keepalive.timer) {
    keepalive.timer = setInterval(() => {
      void pingHealthIfDue();
    }, HEALTH_INTERVAL_MS);
    keepalive.timer.unref?.();
  }

  void pingHealthIfDue();
}

