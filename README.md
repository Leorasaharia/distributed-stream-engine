# Distributed Stream Processing Engine

[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]

> A production-ready, highly scalable streaming engine prototype designed to mimic the core capabilities of **Apache Flink** & **Spark Streaming**.

This system seamlessly ingests, orchestrates, and aggregates high-throughput real-time events across a heterogeneous microservice stack leveraging **Java Spring Boot**, **Python Celery**, **Apache Kafka**, **Redis**, and a native **Kotlin Desktop UI**.

---

## Architecture Stack

This project is decoupled by design, isolating event ingestion from stateful computations to ensure **low latency, fault tolerance, and horizontal scalability**.

* **Event Bus (Apache Kafka + Zookeeper):** Acts as the durable, high-throughput buffer to absorb massive event streams.
* **Stream Engine (Java Spring Boot):** Consumes Kafka topics securely, translates the payload into the native *Celery v2 Messaging Protocol*, and acts as the orchestrator to route traffic to the worker tier.
* **Worker Cluster (Python Celery):** A horizontally scalable consumer group that retrieves workloads directly from the Redis broker. It manages local memory bounds via strict prefetching.
* **State Store (Redis):** Acts as both a message broker and an ultra-fast state repository holding idempotency locks and sliding-window aggregations.
* **Producer (Python):** An infinite generator that simulates thousands of JSON events (clicks, purchases, views) per minute.
* **Frontend (Kotlin / Jetpack Compose):** A modern, dark-themed native Desktop Dashboard that polls the engine for live metrics.

---

## Key Capabilities

### Fault Tolerance & Retries
The pipeline guarantees **At-Least-Once** processing. If a Python Celery worker crashes mid-execution, the task remains unacknowledged within Redis and is safely routed to surviving workers. The Spring Boot consumer securely manages Kafka offset commits internally ensuring messages are never lost during component restarts.

### Idempotent Safeguards
Distributed systems occasionally duplicate events across network partitions. Our cluster guards against this strictly in the worker layer, hashing a combination of user signals and timestamps into a temporary TTL lock in Redis. Duplicate computations are preemptively rejected.

### Event-Time Sliding Windows
Instead of processing events strictly based on server time, the cluster honors the payload's encoded `timestamp` (Event-Time semantics). It maintains a real-time **10-second Sliding Window** inside Redis using Sorted Sets (`ZADD`, `ZREMR` bounds), intelligently dropping late-arriving fragments while summing up active windows on the fly!

### Backpressure Handling
* **Kafka** naturally buffers consumer overload.
* The **Celery Workers** are constrained with `worker_prefetch_multiplier=1` to prevent a single worker from greedily pulling down too many tasks, distributing CPU load predictably avoiding OOM crashes.

---

## Quickstart Guide

Getting the cluster up and running is entirely containerized!

### 1. Boot the Backend Services
Make sure you have Docker Desktop installed. Navigate to the project root and spin up the multi-container ecosystem:

```bash
docker-compose up -d --build
```
*This instantly launches Zookeeper, Kafka, Redis, the Spring Boot Engine, the Celery Workers, and the Mock Python Producer.*

### 2. Verify the API Integration
Within seconds of the deployment, the producer will flood the system with events. Check the endpoints from your terminal:

* **Engine Statistics (Dispatched/Processed records):**
  ```bash
  curl http://localhost:8080/api/jobs/status
  ```
* **Real-time User Aggregations:**
  ```bash
  curl http://localhost:8080/api/jobs/metrics
  ```

### 3. Launch the Native Dashboard
To view the streaming data visually:
1. Open up **IntelliJ IDEA** (or Android Studio).
2. Select **File -> Open...** and target the `kotlin-frontend` folder located in this repository.
3. Allow the IDE to automatically download Gradle dependencies and index the project.
4. Navigate into `src/main/kotlin/Main.kt`.
5. Click the **green Run arrow** next to `fun main()` to launch the Desktop Dashboard! 

---

## Directory Structure

```text
├── docker-compose.yml       # Entire backend orchestrator
├── stream-engine/           # Spring Boot Application (Kafka -> Celery Pipeline)
├── worker/                  # Python Celery Cluster (Stateful Aggregations)
├── producer/                # Python Script (Mock JSON event generator)
├── kotlin-frontend/         # Jetpack Compose for Desktop UI Project
└── docs/                    
    └── architecture.md      # Extended engineering decisions
```

---


<!-- MARKDOWN LINKS -->
[stars-shield]: https://img.shields.io/github/stars/yourname/distributed-stream-engine.svg?style=for-the-badge
[stars-url]: https://github.com/yourname/distributed-stream-engine/stargazers
[issues-shield]: https://img.shields.io/github/issues/yourname/distributed-stream-engine.svg?style=for-the-badge
[issues-url]: https://github.com/yourname/distributed-stream-engine/issues
[license-shield]: https://img.shields.io/github/license/yourname/distributed-stream-engine.svg?style=for-the-badge
[license-url]: https://github.com/yourname/distributed-stream-engine/blob/master/LICENSE.txt
