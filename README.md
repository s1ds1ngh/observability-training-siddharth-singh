# Observability Training - Nashtech

## Day 1: Foundations Workshop

### Project Overview

This project is an **Observable Inventory Service** built with Spring Boot 3.4 that demonstrates core observability practices including structured logging, application metrics, and visualization.

#### Service Description
A REST API that simulates an inventory lookup service exposing a `GET /api/inventory/{itemId}` endpoint. It returns simulated inventory data including item name, quantity, and price.

#### Simulated Error Scenarios
To generate realistic telemetry data, the service introduces:
- **Random Processing Delays** (~30% of requests): Simulates slow database queries or downstream service latency with delays of 100–600ms
- **Random Internal Server Errors** (~20% of requests): Simulates `HTTP 500` errors from unexpected failures, returning error details in the response

---

### Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Application | Spring Boot 3.4.1 (Java 21) | REST API service |
| Metrics | Micrometer + Prometheus Registry | Custom & JVM metrics |
| Logging | Logback + Logstash Encoder | Structured JSON logs |
| Log Aggregation | Loki + Promtail | Centralized log collection |
| Metrics DB | Prometheus | Metrics scraping & storage |
| Visualization | Grafana | Dashboards & log exploration |

---

### Observability Features

#### 1. Structured Logging (JSON)
- All logs output in JSON format via `logstash-logback-encoder`
- MDC (Mapped Diagnostic Context) fields: `requestId`, `itemId`, `endpoint`
- Log levels: `INFO` (request entry/exit), `DEBUG` (processing details), `ERROR` (exceptions)

#### 2. Custom Metrics
- **Counter**: `inventory_requests_total` with dynamic `status` tag (`success` | `error`)
- **Actuator Endpoint**: `/actuator/prometheus` exposes all metrics for scraping

#### 3. MDC Usage
- Each request gets a unique `requestId` (UUID) for correlation
- `itemId` and `endpoint` are set in MDC for every log line
- MDC is cleared in a `finally` block to prevent context leakage

---

### How to Run

#### Prerequisites
- Java 21
- Maven 3.6+
- Docker & Docker Compose

#### Run Locally (without Docker)
```bash
cd day1-foundations-workshop
mvn spring-boot:run
```
Hit the endpoint:
```bash
curl http://localhost:8080/api/inventory/42
```

#### Run with Full Observability Stack
```bash
cd day1-foundations-workshop
docker compose up -d --build
```

| Service | URL |
|---------|-----|
| Application | http://localhost:8080/api/inventory/1 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Loki | http://localhost:3100 |

#### Generate Traffic
```bash
for i in $(seq 1 100); do curl -s http://localhost:8080/api/inventory/$((RANDOM % 50 + 1)) > /dev/null; done
```

#### Run Tests
```bash
cd day1-foundations-workshop
mvn test
```

---

### Visual Proof

#### 1. Grafana Dashboard - Custom Prometheus Counter
Screenshot from 2026-03-16 12-07-55.png

#### 2. Grafana Explore - Loki JSON Structured Logs
Screenshot from 2026-03-16 12-03-38.png


---

### Project Structure
```
day1-foundations-workshop/
├── src/
│   ├── main/
│   │   ├── java/com/nashtech/observability/
│   │   │   ├── ObservabilityApplication.java
│   │   │   └── controller/
│   │   │       └── InventoryController.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/nashtech/observability/controller/
│           └── InventoryControllerTest.java
├── prometheus/prometheus.yml
├── promtail/promtail-config.yml
├── grafana/provisioning/datasources/datasources.yml
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## Day 2: Advanced Workshop - Distributed Tracing

This folder `day2-advanced-workshop` extends the Day 1 inventory service with **distributed tracing** using **Micrometer Tracing**, **OpenTelemetry**, **OpenTelemetry Collector**, and **Grafana Tempo**.

### Tracing Architecture

- Application (Spring Boot + Micrometer Tracing)
- OpenTelemetry OTLP exporter (via Spring Boot management.otlp.tracing)
- OpenTelemetry Collector (`otel-collector` service in Docker Compose)
- Grafana Tempo (`tempo` service in Docker Compose)
- Grafana for Tempo trace visualization

### Key Additions

- **Distributed Tracing**: Micrometer Tracing + OpenTelemetry bridge and OTLP exporter dependencies in `pom.xml`.
- **Tracing Configuration**: `management.tracing.*` and `management.otlp.tracing.endpoint` properties in `application.properties`.
- **Log–Trace Correlation**: `logback-spring.xml` updated so every JSON log includes `traceId` and `spanId`.
- **Custom Span**: `InventoryController` creates a nested span (`validate-inventory`) with attribute `item.id={itemId}` to tag each request.
- **OTel Collector & Tempo**: `docker-compose.yml` extended with `otel-collector` and `tempo` services to receive and store traces.

### How to Run Day 2 Stack

```bash
cd day2-advanced-workshop
docker compose up -d --build
```

Services:

- Application: `http://localhost:8080/api/inventory/1`
- Grafana: `http://localhost:3001` (admin/admin)
- Tempo: accessed via Grafana Tempo data source

Generate traffic and error traces:

```bash
for i in $(seq 1 100); do curl -s http://localhost:8080/api/inventory/$((RANDOM % 50 + 1)) > /dev/null; done
```

Use the random 500 errors and the `traceId`/`spanId` fields in Loki logs to locate the corresponding traces in Tempo.

### Visual Proof (to be attached by you)

Please capture and attach the following screenshots before submission:

1. Grafana Tempo Gantt chart showing the full request trace, including the custom `validate-inventory` span.
2. A trace with a failed span (red) caused by a simulated HTTP 500 error.
3. Grafana Explore view showing a JSON log containing `traceId` and `spanId`, alongside the matching trace opened in Tempo.
