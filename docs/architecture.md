# Distributed Stream Processing Engine

## Architecture Overview

This project implements a distributed stream processing engine prototype modeled after real-time data processing systems like Apache Flink or Kafka Streams. It is designed to handle high-throughput event streams with low latency, fault tolerance, and horizontal scalability.

### Data Flow Pipeline
1. **Producer:** A Python script continuously generates simulated structured JSON events (e.g., clicks, views, purchases) and publishes them to an Apache Kafka topic (`events-topic`).
2. **Stream Engine (Spring Boot):** Consumes events from Kafka via `EventIngestionService`. It acts as the coordinator and safely forwards the event workload to a distributed worker cluster through a Redis broker using the `JobCoordinator`, mocking the Celery protocol over a Redis List.
3. **Worker Layer (Python Celery):** Celery workers pick up tasks from Redis. They provide stateful processing, including sliding & tumbling window aggregations, tracking user metrics, and handling event-time semantics.
4. **State Store (Redis):** Acts both as a message broker for Celery and as a low-latency state backend to store aggregations, idempotency keys, and metrics.

## Key Mechanisms

### 1. Backpressure Handling
* **Kafka Consumer Polling:** Spring Kafka acts as a natural buffer. If the Spring Engine is overloaded, it auto-pauses polling.
* **Celery Prefetch Limits:** The workers are configured with `worker_prefetch_multiplier=1`, preventing them from pulling more tasks than they can immediately execute from Redis. This distributes the load fairly and predictably across horizontally scaled Celery workers.
* **Component Decoupling:** The decoupled architecture (Kafka -> Spring -> Redis -> Celery) provides independent scaling of ingestion vs. processing layers.

### 2. Fault Tolerance & Retries
* **Kafka Offsets:** The Spring Boot consumer manages offsets. The application is configured to resume from the last committed offset in case of crashes or restarts (`auto-offset-reset: earliest` for new groups).
* **Celery Task Backoff:** The workers retry failed computational tasks automatically with exponential backoff (`raise self.retry(...)`).
* **At-Least-Once Guarantee:** Configured `task_acks_late=True` within Celery ensures that a task is only acknowledged and removed from Redis once it has been fully processed, meaning process crashes mid-execution will not lose the event.

### 3. Idempotent Processing
Every streaming engine must handle duplicates efficiently. We enforce idempotency in the Celery worker (`state_manager.py`) by maintaining a TTL-backed record of `{user_id}_{timestamp}` in Redis. This prevents double-counting metrics if a component resends an identical payload due to network retries.

### 4. Windowed Computations (Event-time)
Sliding Windows are implemented inside Redis using Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`). The key is based on the logic of:
1. Inserting the event with its *Event Time* (timestamp within payload) as the score.
2. Evicting all items with a score older than `current_event_time - window_size`.
3. Calculating the sum/aggregate of the remaining elements.
This process cleanly handles late-arriving events within the window timeframe dynamically.

## Setup and Run Instructions

### Prerequisites
- Docker and Docker Compose

### 1. Start the Platform
Navigate to the project root directory and run:

```bash
docker-compose up --build
```

This will automatically start Zookeeper, Kafka, Redis, the Spring Boot Engine, Celery Workers, and the Event Producer.

### 2. Verify Metrics
Once the systems are running and the producer is generating events, you can query the live metrics through the Spring Boot REST API.

**Check the job ingestion status:**
```bash
curl http://localhost:8080/api/jobs/status
```

**Check the real-time aggregations (user metrics):**
```bash
curl http://localhost:8080/api/jobs/metrics
```

### 3. Monitoring Components
- Verify Celery worker logs to see successful idempotency checks and sliding window aggregations.
- Use `redis-cli` within the redis container to inspect keys via `keys *` and raw metrics.
