export const API_BASE_URL =
  process.env.LOGSTORE_API_BASE_URL ??
  (process.env.LOGSTORE_API_HOSTPORT
    ? `http://${process.env.LOGSTORE_API_HOSTPORT}`
    : "http://127.0.0.1:8080");

