import { NextResponse } from "next/server";

const API_BASE_URL =
  process.env.LOGSTORE_API_BASE_URL ??
  (process.env.LOGSTORE_API_HOSTPORT
    ? `http://${process.env.LOGSTORE_API_HOSTPORT}`
    : "http://127.0.0.1:8080");

export async function GET() {
  try {
    const response = await fetch(`${API_BASE_URL}/cluster`, {
      cache: "no-store"
    });
    const text = await response.text();
    return new NextResponse(text, {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("Content-Type") ?? "application/json"
      }
    });
  } catch (error) {
    return NextResponse.json(
      {
        error:
          error instanceof Error ? error.message : "Failed to reach LogStore backend"
      },
      { status: 502 }
    );
  }
}
