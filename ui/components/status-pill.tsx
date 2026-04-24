function toneForStatus(status: string): "healthy" | "warning" | "danger" | "info" {
  const value = status.toLowerCase();
  if (["up", "active", "healthy", "replicated"].includes(value)) {
    return "healthy";
  }
  if (["degraded", "pending", "idle", "empty"].includes(value)) {
    return "warning";
  }
  if (["down", "failed", "error"].includes(value)) {
    return "danger";
  }
  return "info";
}

export function StatusPill({ status }: { status: string }) {
  return (
    <span className="status-pill" data-tone={toneForStatus(status)}>
      {status}
    </span>
  );
}
