# LogStore

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/sauravjaiswalsj/LogStore)

**LogStore is an experimental embedded commit log for Java services.**

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
* a path to optional replication later

Kafka is excellent for large distributed streaming systems, but it introduces a broker cluster and operational complexity. Chronicle Queue is excellent as an embedded Java queue, but it is not a distributed commit log.

LogStore sits between them:

| System | Embedded | Distributed | Primary Fit |
|---|---:|---:|---|
| Apache Kafka | No | Yes | Large-scale event streaming |
| Chronicle Queue | Yes | No | Low-latency local persisted queues |
| LogStore | Yes | Planned | Embedded durable logs before full streaming infrastructure |

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
* Per-tablet persistent single-writer pipeline
* `FSYNC_EVERY_WRITE`, `BATCHED_FSYNC`, and `ASYNC_FLUSH` durability modes
* Bounded append queues with block/reject backpressure
* Sparse offset index for faster reads from known offsets
* Persistent disk-backed storage
* Spring Boot server wrapper
* Static 3-node replication demo with leader/follower log shipping and quorum ack
* Operator UI for inspecting tablets, offsets, and cluster state

---

## Quick Start

### Embedded Java API

```java
import com.logstore.core.api.AppendResult;
import com.logstore.core.api.Durability;
import com.logstore.core.api.LogRecord;
import com.logstore.core.api.LogStore;
import com.logstore.core.api.LogStoreConfig;

import java.nio.file.Path;
import java.util.List;

LogStore store = LogStore.open(LogStoreConfig.builder()
    .dataDir(Path.of("./data/logstore"))
    .partitions(16)
    .durability(Durability.BATCHED_FSYNC)
    .batchSize(128)
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
./mvnw -pl logstore-server spring-boot:run
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
  "stream": "orders",
  "key": "ORD-1",
  "value": "{\"event\":\"OrderCreated\",\"orderId\":\"ORD-1\"}"
}
```

Response:

```json
{
  "stream": "orders",
  "offset": 42,
  "tabletId": 3
}
```

Read records:

```http
GET /read?stream=orders&offset=0&limit=100
```

---

## Alpha Scope

The current alpha covers V0.1 through V0.3:

* V0.1: embedded append/read/replay, framed records, restart recovery, and Spring Boot integration.
* V0.2: persistent per-tablet writers, batched fsync, async flush mode, sparse offset indexes, bounded queues, and JMH benchmark scaffolding.
* V0.3: static leader/follower replication demo with quorum acknowledgement and follower catch-up.

Automatic leader election is intentionally out of scope. Kill-leader behavior is unsupported in this alpha and should be handled manually by changing configuration and restarting nodes.

The protobuf service contract lives at `logstore-server/src/main/proto/logstore.proto`. Peer replication uses gRPC for `Replicate`, `FetchFromOffset`, and `ClusterStatus`; Spring HTTP remains the public/demo surface for client appends, reads, health, and UI calls.

---

## Architecture

```text
                 Embedded Java API / REST
                              |
                              v
                         LogStore Core
                              |
                +-------------+-------------+
                |                           |
          Stream Router
                |
                v
       stream -> tablet/partition
                |
                v
       append-only tablet files
                |
                v
        recovery + offset replay
```

### Storage Model

```text
stream -> tablet -> framed records
```

Normal users interact with streams and offsets. Tablets and segments are internal implementation details exposed only through admin APIs and the UI.

### Concurrency Model

LogStore serializes append operations through a persistent writer per tablet:

```text
caller threads -> bounded tablet queue -> single tablet writer -> persistent FileChannel
```

This preserves ordering within a tablet while allowing parallel writes across tablets.

### Record Format

Records are stored as binary frames:

```text
magic | version | length | offset | timestamp | streamLength | keyLength | valueLength | crc32 | stream | key | value
```

This format supports:

* safe recovery after crashes
* detection of partial/corrupt records
* binary payloads
* efficient sequential reads

---

## Durability

| Mode | Description | Use Case |
|---|---|---|
| `FSYNC_EVERY_WRITE` | Force data to disk on every append | strongest local durability |
| `BATCHED_FSYNC` | Force after `batchSize` records or `flushIntervalMillis` | benchmark/default alpha performance mode |
| `ASYNC_FLUSH` | Write through the OS page cache without forcing each append | highest throughput, weakest crash durability |

---

## Benchmarks

Build and run the JMH benchmark:

```bash
./mvnw -pl logstore-benchmarks -am test-compile dependency:build-classpath \
  -Dmdep.outputFile=logstore-benchmarks/target/classpath.txt
java -cp "logstore-benchmarks/target/classes:$(cat logstore-benchmarks/target/classpath.txt)" \
  org.openjdk.jmh.Main EmbeddedAppendBenchmark
```

No throughput number should be treated as a claim until it includes the command, machine, JVM version,
payload size, partitions, and durability mode.

| Benchmark | Status |
|---|---|
| Embedded append JMH | implemented |
| HTTP append benchmark | planned |
| Published throughput table | pending local measured run |

## Static Replication Demo

The V0.3 demo uses a static leader and two followers:

```bash
docker compose up --build
```

Services:

* Leader node 1: `http://localhost:8080`
* Follower node 2: `http://localhost:8081`
* Follower node 3: `http://localhost:8082`
* gRPC node 1: `localhost:9091`
* gRPC node 2: `localhost:9092`
* gRPC node 3: `localhost:9093`
* UI: `http://localhost:3000`

Append to the leader:

```bash
curl -X POST http://localhost:8080/append \
  -H 'Content-Type: application/json' \
  -d '{"stream":"orders","key":"ORD-1","value":"created"}'
```

Read from a follower:

```bash
curl 'http://localhost:8081/read?stream=orders&offset=0&limit=10'
```

Check cluster status:

```bash
curl http://localhost:8080/cluster/status
```

With `LOGSTORE_ACK_MODE=QUORUM`, the leader returns success after the local write plus at least one follower persist over gRPC. If one follower is down, appends can continue while quorum is still available. When the follower returns, it pulls missing records from the leader with `FetchFromOffset`; the leader also catch-ups lagging followers before retrying a current replication.

---

## Docker

Run the 3-node alpha cluster and UI:

```bash
docker compose up --build
```

The UI talks to node 1 through `LOGSTORE_API_BASE_URL=http://logstore-node-1:8080`.

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
├── logstore-core
│   └── src/main/java/com/logstore/core
│       ├── api           # public embedded Java API
│       ├── storage       # tablets, segments, encoding, decoding, recovery
│       └── util          # CRC and hashing helpers
├── logstore-server
│   └── src/main/java/com/projects/logstore
│       ├── controller    # REST endpoints
│       ├── server        # Spring Boot-facing services
│       ├── replication   # distributed alpha components
│       └── cluster       # cluster metadata and health
├── logstore-benchmarks   # benchmark module placeholder
├── script                # load and stress tests
├── ui                    # Next.js operator console
├── data/logstore         # local log data
├── docker-compose.yml
├── render.yaml
└── pom.xml
```

---

## Tech Stack

* Java 23
* Spring Boot
* Maven
* Java NIO `FileChannel`
* Next.js operator UI
* Docker / Render deployment
* gRPC / Protobuf replication transport

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
