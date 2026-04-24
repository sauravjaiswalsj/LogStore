export function formatNumber(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

export function formatBytes(value: number): string {
  if (value < 1024) {
    return `${value} B`;
  }

  const units = ["KB", "MB", "GB", "TB"];
  let current = value / 1024;
  let index = 0;

  while (current >= 1024 && index < units.length - 1) {
    current /= 1024;
    index += 1;
  }

  return `${current.toFixed(current < 10 ? 1 : 0)} ${units[index]}`;
}

export function formatDateTime(value: string | number | null): string {
  if (!value) {
    return "No activity";
  }

  const date = typeof value === "number" ? new Date(value) : new Date(value);
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "medium",
    timeZone: "UTC"
  }).format(date);
}

export function formatRelativeTime(value: string | number | null): string {
  if (!value) {
    return "No activity";
  }

  const date = typeof value === "number" ? new Date(value) : new Date(value);
  const diff = date.getTime() - Date.now();
  const minutes = Math.round(diff / 60000);
  const rtf = new Intl.RelativeTimeFormat("en", { numeric: "auto" });

  if (Math.abs(minutes) < 60) {
    return rtf.format(minutes, "minute");
  }

  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) {
    return rtf.format(hours, "hour");
  }

  const days = Math.round(hours / 24);
  return rtf.format(days, "day");
}
