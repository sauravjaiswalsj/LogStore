# LogStore

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/sauravjaiswalsj/LogStore)

**LogStore is an embedded distributed commit log for Java services.**

It gives applications a durable, ordered, replayable event log without requiring an external Kafka cluster for small and medium-scale workflows.

> Embedded first. Distributed when needed. Simple API always.

LogStore is designed for backend engineers who need append-only event storage for audit trails, event-sourced workflows, durable outbox patterns, and replayable business processes.

---

## Why LogStore?

Modern services often need more than application logs:

* a durable record of business events
* ordered replay from a known offset
* crash recovery from local disk
* low operational overhead
* optional replication for stronger durability

Kafka is excellent for large distributed streaming systems, but it introduces a broker cluster and operational complexity. Chronicle Queue is excellent as an embedded Java queue, but it is not a distributed commit log.

LogStore sits between them:

| System | Embedded | Distributed | Primary Fit |
|---|---:|---:|---|
| Apache Kafka | No | Yes | Large-scale event streaming |
| Chronicle Queue | Yes | No | Low-latency local persisted queues |
| LogStore | Yes | Yes | Embedded durable logs with optional replication |

LogStore is not a Kafka replacement. It is an embedded commit log for applications that want durable, replayable event storage before they need full streaming infrastructure.

---

## Core Features

* Plain Java embedded API
* Append-only immutable records
* Stream-based append/read/replay
* Offset-based reads
* Partitioned storage using tablets
* Binary framed record format with CRC validation
* Startup recovery from existing log files
* Per-tablet single-writer concurrency model
* Configurable durability modes
* Persistent disk-backed storage
* Spring Boot server wrapper
* Operator UI for inspecting tablets, offsets, and cluster state
* Static 3-node replicated alpha mode with leader/follower log shipping

---

## Quick Start

### Embedded Java API

```java
import com.projects.logstore.core.AppendResult;
import com.projects.logstore.core.Durability;
import com.projects.logstore.core.LogRecord;
import com.projects.logstore.core.LogStore;
import com.projects.logstore.core.LogStoreConfig;

import java.nio.file.Path;
import java.util.List;

LogStore store = LogStore.open(LogStoreConfig.builder()
    .dataDir(Path.of("./data/logstore"))
    .partitions(16)
    .durability(Durability.BATCHED_FSYNC)
    .flushIntervalMillis(5)
    .build());

AppendResult result = store.append(
    "orders",
    "ORD-1",
    """
    {"event":"OrderCreated","orderId":"ORD-1"}
    """.getBytes()
);

List<LogRecord> records = store.read("orders", 0, 100);

store.close();
```

### Spring Boot Server

```bash
./mvnw spring-boot:run
```

Endpoints:

* Backend: `http://localhost:8080`
* Swagger: `http://localhost:8080/swagger-ui.html`
* Health: `http://localhost:8080/health`

Append a record:

```http
POST /append
Content-Type: application/json

{
  "key": "ORD-1",
  "value": "{\"event\":\"OrderCreated\",\"orderId\":\"ORD-1\"}"
}
```

Response:

```json
{
  "stream": "default",
  "offset": 42,
  "tabletId": 3
}
```

Read records:

```http
GET /read?stream=default&offset=0&limit=100
```

---

## Distributed Alpha Mode

LogStore can run as a static replicated cluster for local demos and alpha testing.

The V1 alpha distributed mode uses:

* static 3-node membership
* configured leader/follower roles
* leader-owned offset assignment
* follower append replication
* quorum acknowledgements
* follower catch-up from offset

Automatic leader election is intentionally not part of this alpha. The goal is to prove the embedded-distributed storage model before adding full consensus machinery.

Example cluster configuration:

```java
LogStoreCluster cluster = LogStoreCluster.open(ClusterConfig.builder()
    .nodeId("node-1")
    .dataDir(Path.of("./data/node-1"))
    .peers(List.of("node-1:9091", "node-2:9091", "node-3:9091"))
    .leader(true)
    .replicationFactor(3)
    .ackMode(AckMode.QUORUM)
    .build());
```

Replication flow:

```text
client append
    |
    v
leader tablet writer
    |
    +--> local append
    +--> follower 1 append
    +--> follower 2 append
    |
    v
ack after configured durability level
```

---

## Architecture

```text
                 Embedded Java API / REST / gRPC
                              |
                              v
                         LogStore Core
                              |
                +-------------+-------------+
                |                           |
          Stream Router                Cluster Layer
                |                           |
                v                           v
       key -> tablet/partition       leader/follower sync
                |
                v
       single writer per tablet
                |
                v
       segmented append-only files
                |
                v
        recovery + offset replay
```

### Storage Model

```text
stream -> key hash -> tablet -> segment file -> framed records
```

Normal users interact with streams and offsets. Tablets and segments are internal implementation details exposed only through admin APIs and the UI.

### Concurrency Model

LogStore uses one append pipeline per tablet:

```text
caller threads -> tablet queue -> single tablet writer -> disk
```

This preserves ordering within a tablet while allowing parallel writes across tablets.

### Record Format

Records are stored as binary frames:

```text
magic | version | length | offset | timestamp | keyLength | valueLength | crc32 | key | value
```

This format supports:

* safe recovery after crashes
* detection of partial/corrupt records
* binary payloads
* efficient sequential reads

---

## Durability Modes

| Mode | Description | Use Case |
|---|---|---|
| `FSYNC_EVERY_WRITE` | Force data to disk on every append | strongest local durability |
| `BATCHED_FSYNC` | Flush after a batch size or time interval | balanced latency and throughput |
| `ASYNC_FLUSH` | Let the OS flush in the background | fastest mode, accepts recent-data-loss risk |

Durability is explicit because there is no honest single setting that is best for every workload.

---

## Benchmarks

LogStore reports benchmarks by workload, payload size, durability mode, and hardware.

Benchmark layers:

* embedded core benchmark with JMH
* Spring Boot HTTP append benchmark
* gRPC append/replication benchmark
* recovery benchmark

Metrics:

* writes/sec
* p50 latency
* p95 latency
* p99 latency
* failure rate
* payload size
* JVM version
* disk type
* durability mode

No throughput number should be interpreted without its durability mode. `FSYNC_EVERY_WRITE` and `BATCHED_FSYNC` measure very different tradeoffs.

---

## Docker

Run the backend and UI:

```bash
docker compose up --build
```

Services:

* Backend: `http://localhost:8080`
* Swagger: `http://localhost:8080/swagger-ui.html`
* UI: `http://localhost:3000`

The UI talks to the backend through `LOGSTORE_API_BASE_URL=http://backend:8080`.

---

## Render Deployment

This repo includes a Render blueprint in `render.yaml`.

Recommended setup:

* `logstore-backend` as a Docker web service
* `logstore-ui` as a Docker web service
* persistent disk mounted at `/app/data/logstore` for the backend

Important: LogStore writes local data files. Without a persistent disk, data can be lost on redeploy or restart.

---

## Use Cases

### Audit Logging

Store immutable business events for compliance, debugging, and traceability.

### Event Sourcing

Replay events from offset `0` to rebuild application state.

### Durable Outbox

Persist outbound events before forwarding them to another system.

### Workflow History

Track state transitions for orders, claims, payments, and operational workflows.

### Local Event Bus

Decouple components inside a single service while retaining durable replay.

---

## What LogStore Is Not

LogStore is not:

* a Kafka replacement
* a message queue with consumer groups
* a search engine
* a telemetry platform
* a schema registry
* an exactly-once transaction system

LogStore is:

> an embedded commit log for Java applications, with optional replicated alpha mode.

---

## Project Structure

```text
LogStore/
├── src/main/java/com/projects/logstore
│   ├── core              # embedded log API and storage engine
│   ├── server            # Spring Boot-facing services
│   ├── controller        # REST endpoints
│   ├── replication       # distributed alpha components
│   ├── cluster           # cluster metadata and health
│   ├── tablet            # partition/tablet internals
│   └── storage           # append/read storage primitives
├── src/test/java         # correctness and recovery tests
├── script                # load and stress tests
├── ui                    # Next.js operator console
├── data/logstore         # local log data
├── docker-compose.yml
├── render.yaml
└── pom.xml
```

---

## Tech Stack

* Java 17+
* Spring Boot
* Maven
* Java NIO `FileChannel`
* gRPC/Protobuf for distributed alpha mode
* Next.js operator UI
* Docker / Render deployment

---

## Roadmap

* automatic leader election
* persistent index files
* log compaction
* snapshots
* consumer cursors
* Java client artifact
* Spring Boot starter
* Go/Rust clients over gRPC

---

## License

Apache-2.0 License
