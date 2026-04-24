import { Panel } from "@/components/panel";

const REQUESTS = [
  {
    method: "POST",
    path: "/append",
    body: '{ "key": "user-123", "value": "CREATE_TASK" }'
  },
  {
    method: "GET",
    path: "/read?tabletId=2&startOffset=1042&limit=10",
    body: "No body"
  },
  {
    method: "GET",
    path: "/tablets",
    body: "Returns live tablet summaries for the dashboard."
  },
  {
    method: "GET",
    path: "/cluster",
    body: "Returns current cluster posture and pending capabilities."
  }
];

export function RequestsPanel() {
  return (
    <Panel
      title="Requests"
      description="A lightweight operator cheat sheet for the backend surfaces powering this UI."
    >
      <div className="timeline">
        {REQUESTS.map((request) => (
          <div key={`${request.method}-${request.path}`} className="timeline-item">
            <div className="split-line">
              <strong className="mono">
                {request.method} {request.path}
              </strong>
            </div>
            <p>{request.body}</p>
          </div>
        ))}
      </div>
    </Panel>
  );
}
