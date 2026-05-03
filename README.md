# A Distributed Append-Only Log Store (Kafka - scratch version)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/sauravjaiswalsj/LogStore)
## 📌 Overview

This project is a **simplified Kafka-like distributed append-only log system** built from scratch using **Java 23 and Spring Boot**.

It demonstrates how **high-throughput, ordered writes** can be achieved by enforcing **single-leader writes per partition**, eliminating locks while maintaining correctness, durability, and scalability.

> **Core idea:**
> *Ordering is enforced by a single leader per partition. All writes are append-only and sequential, enabling lock-free, high-performance ingestion.*

---
<img width="1665" height="910" alt="Image" src="https://github.com/user-attachments/assets/4663fbf6-efb5-4b86-ac07-2d4e36399e37" />

## 🎯 Goals of the Project

* Understand **append-only log architecture**
* Learn **partitioned concurrency without locks**
* Implement **leader–follower replication**
* Demonstrate **crash recovery using log replay**
* Build a **systems-level project**, not CRUD

---

## 🧠 Key Concepts Implemented

| Concept            | Description                                           |
| ------------------ | ----------------------------------------------------- |
| Append-only log    | Data is never updated or deleted in place             |
| Partitioning       | Data is sharded into independent tablets              |
| Single leader      | One writer per partition ensures ordering             |
| Sequential I/O     | Disk writes are sequential, not random                |
| Offset-based reads | Consumers read using monotonically increasing offsets |
| Replication        | Leader replicates log entries to followers            |
| Crash recovery     | Logs are replayed on startup                          |

---

## 🏗️ Architecture

### High-Level Architecture Diagram

<img width="736" height="801" alt="Image" src="https://github.com/user-attachments/assets/e03fa2f8-52dd-4937-9604-18f2e87fd731" />

---### Component Interaction

```
                        ┌────────────┐
                        │   Client   │
                        └─────┬──────┘
                              │
                              v
                ┌─────────────────────────┐
                │     TabletServer         │
                │   (Leader Instance)      │
                └─────┬───────────┬───────┘
                      │           │
          ┌───────────v───┐   ┌───v───────────┐
          │  Tablet A     │   │  Tablet B     │
          │ (Partition)  │   │ (Partition)   │
          └─────┬────────┘   └─────┬─────────┘
                │                  │
        Append-only Log      Append-only Log
        (Sequential I/O)     (Sequential I/O)
                │ (gRPC)-high rate │
        ┌───────v────────┐ ┌───────v────────┐
        │ Replica Server │ │ Replica Server │
        │  (Follower)    │ │  (Follower)    │
        └────────────────┘ └────────────────┘
```
---

### Core Architectural Principles

* **Each tablet = one partition**
* **Exactly one leader per tablet**
* **Only leader writes to the log**
* **Replication is log-based and ordered**
* **Concurrency happens across tablets, not within**

---

## 🔑 Why No Locks Are Needed

* A tablet has **exactly one leader**
* Only the leader thread appends to its log
* Offset assignment is a **monotonic counter**
* No shared mutable state between tablets

Result:

* No race conditions
* No file-level locks
* Extremely high write throughput

---

## 📂 Log Record Format

Each log entry is stored sequentially in the following format:

```
[offset][timestamp][key_length][key][value_length][value]
```

* `offset` → Monotonically increasing
* Records are immutable
* New entries are appended at the end of the file

### Example

0|1734150400123|user-123|CREATE_TASK
1|1734150400456|user-456|UPDATE_TASK

Records are append-only and written sequentially to disk.
Offsets are strictly increasing and never reused.
---

## 🌐 REST APIs [Swagger](http://localhost:8080/swagger-ui.html)

### Append Record

```http
POST /append
{
  "key": "user123",
  "value": "event-data"
}
```

**Response**

```json
{
  "tabletId": 2,
  "offset": 1042
}
```

---

### Read Record

```http
GET /read?tabletId=2&offset=1042
```

## 🐳 Docker

Run the backend and Next.js UI together:

```bash
docker compose up --build
```

Endpoints:

* Backend → `http://localhost:8080`
* Swagger → `http://localhost:8080/swagger-ui.html`
* UI → `http://localhost:3000`

The UI talks to the backend through the Compose network using `LOGSTORE_API_BASE_URL=http://backend:8080`.

## ☁️ Deploy To Render

This repo now includes a Render blueprint at `render.yaml`.

Recommended setup:

* `logstore-backend` as a Docker web service
* `logstore-ui` as a Docker web service
* A persistent disk mounted at `/app/data/logstore` for the backend

Deploy steps:

1. Push this repo to GitHub/GitLab.
2. In Render, choose `New +` → `Blueprint`.
3. Select the repo and deploy the root `render.yaml`.

Important notes:

* The backend writes local log files, so it should use a persistent disk on Render. Without one, data is lost on redeploy/restart.
* The included blueprint uses a `starter` plan for the backend because Render persistent disks require a paid service.
* The UI connects to the backend over Render's private network using a service reference, so you do not need to hardcode the backend public URL.
* If you deploy services manually instead of via the blueprint, set `LOGSTORE_API_BASE_URL` on the frontend service to your backend URL, for example `https://logstore.onrender.com`.
* To show the backend link inside the UI, set `NEXT_PUBLIC_BACKEND_URL` on the frontend service to the same public backend URL.

---

## 🔁 Replication Model

* Each tablet consists of:

    * **1 leader**
    * **N replicas**
* Workflow:

    1. Leader appends record
    2. Offset is assigned
    3. Record is replicated to followers
    4. Followers append in the same order

This guarantees **ordering, durability, and fault tolerance**.

---

## 💥 Crash Recovery

On startup:

1. Log files are scanned
2. Last offset is recovered
3. Appends resume from the correct position

✔ No data loss
✔ No offset duplication
✔ Safe restarts

---

## 📁 Project Structure

```
mini-log-store/
com.projects.logstore
│
├── config
│   ├── TabletConfig
│   └── TabletProperties
│
├── controller
│   ├── AppendController
│   ├── ReadController
│   └── PingController
│
├── dto
│   ├── AppendDTO
│   ├── ReadDTO
│   └── LogRecord
│
├── model
│   └── Data
│
├── cluster                 ← NEW
│   ├── ClusterManager
│   ├── NodeState
│   ├── NodeRole
│   ├── PeerNode
│   └── TabletReplica
│
├── replication             ← NEW
│   ├── ReplicationManager
│   ├── ReplicationClient
│   ├── ReplicationService
│   ├── ReplicationRequest
│   └── ReplicationResponse
│
├── election                ← NEW (later)
│   ├── LeaderElectionService
│   ├── VoteRequest
│   ├── VoteResponse
│   └── ElectionTimer
│
├── server
│   ├── TabletServer
│   ├── ReplicaReceiver
│   └── LeaderReplication
│
├── tablet
│   ├── Tablet
│   ├── TabletRouter
│   └── RegistryTable
│
├── storage
│   ├── impl
│   │   ├── AppendOnlyLogService
│   │   └── ReadOnlyLogService
│
├── recovery
│   └── LogReplayService
│
└── LogStoreApplication
├── src/main/resources/
│   └── application.yml
│
├── README.md
└── pom.xml
```

---

## 🛠️ Tech Stack

* **Java:** OpenJDK 23.0.1
* **Framework:** Spring Boot
* **Build Tool:** Apache Maven 3.9.11
* **I/O:** FileChannel for sequential disk writes
* **Networking:** gRPC for inter-node communication
* **Protocol:** Client -> REST (HTTP) | Leader <-> Followers (gRPC)

---

## 🚀 Performance Characteristics

* Sequential disk writes
* Append-only storage
* No random disk access
* Lock-free write path
* Horizontal scalability via partitions

---

## 🧪 What This Project Intentionally Excludes

* Raft / Paxos
* Exactly-once semantics
* Transactions
* Schema registry

> These are excluded to keep focus on **core log storage mechanics**.

---

## 🔮 Future Enhancements

* Log compaction
* Segment rolling
* Read replicas
* Metrics & monitoring
* Leader re-election

---

## 📜 License

Apache-2.0 License
