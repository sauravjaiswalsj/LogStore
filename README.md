# A Distributed Append-Only Log Store (Kafka - scratch version)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/sauravjaiswalsj/LogStore)
## 📌 Overview

This project is a **simplified Kafka-like distributed append-only log system** built from scratch using **Java 23 and Spring Boot**.

It demonstrates how **high-throughput, ordered writes** can be achieved by enforcing **single-leader writes per partition**, eliminating locks while maintaining correctness, durability, and scalability.

> **Core idea:**
> *Ordering is enforced by a single leader per partition. All writes are append-only and sequential, enabling lock-free, high-performance ingestion.*

---

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
                │                  │
        ┌───────v────────┐ ┌───────v────────┐
        │ Replica Server │ │ Replica Server │
        │  (Follower)    │ │  (Follower)    │
        └────────────────┘ └────────────────┘
```
or
<img width="829" height="672" alt="Image" src="https://github.com/user-attachments/assets/83f3d6e0-4959-49f0-a342-b02251b983ea" />
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
├── src/main/java/com/example/logstore
│   ├── api/
│   │   ├── AppendController.java
│   │   └── ReadController.java
│   │
│   ├── server/
│   │   ├── TabletServer.java
│   │   ├── LeaderReplicator.java
│   │   └── ReplicaReceiver.java
│   │
│   ├── tablet/
│   │   ├── Tablet.java
│   │   ├── TabletRouter.java
│   │   └── TabletRegistry.java
│   │
│   ├── storage/
│   │   ├── AppendOnlyLog.java
│   │   ├── LogSegment.java
│   │   └── LogReader.java
│   │
│   ├── recovery/
│   │   └── LogReplayService.java
│   │
│   └── MiniLogStoreApplication.java
│
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
* **I/O:** FileChannel / Buffered Streams
* **Protocol:** REST (HTTP)

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

MIT
