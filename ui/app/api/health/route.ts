import { NextResponse } from "next/server";
import { API_BASE_URL } from "@/lib/backend";
import { touchBackendHealthKeepalive } from "@/lib/health-keepalive";

export async function GET() {
  touchBackendHealthKeepalive();

  try {
    const response = await fetch(`${API_BASE_URL}/health`, {
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
