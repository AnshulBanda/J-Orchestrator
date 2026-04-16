# J-Orchestrator — Distributed Task Scheduler

A **Spring Boot 3 / Java 17** reference implementation of a distributed task scheduler,
designed as an OOAD (Object-Oriented Analysis & Design) teaching artefact.
Every architectural decision — from SOLID principles to GRASP patterns and GoF design
patterns — is traceable directly to a named class or interface in the codebase.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Quick Start](#2-quick-start)
3. [Architecture Overview](#3-architecture-overview)
4. [OOAD Concept → Class Mapping](#4-ooad-concept--class-mapping)
   - 4.1 [SOLID Principles](#41-solid-principles)
   - 4.2 [GRASP Patterns](#42-grasp-patterns)
   - 4.3 [GoF Design Patterns](#43-gof-design-patterns)
   - 4.4 [OO Relationships](#44-oo-relationships)
5. [Module Breakdown](#5-module-breakdown)
6. [API Reference](#6-api-reference)
7. [Configuration](#7-configuration)
8. [Project Structure](#8-project-structure)

---

## 1. Project Overview

| Actor | Role |
|---|---|
| **DevOps Engineer** | Submits JAR/Script jobs, views execution logs, cancels stuck tasks |
| **Cluster Admin** | Approves worker nodes, manages resource quotas |
| **Worker Agent** (simulated) | Executes code on a remote machine and reports status/logs back |

### Functional Modules

| # | Module | Spring Bean | Responsibility |
|---|---|---|---|
| 1 | Job Submission | `JobController` + `JobSubmissionService` | Validate, build, and persist job definitions |
| 2 | Node Management | `NodeController` + `NodeRegistryService` | Heartbeat tracking; OFFLINE demotion; approval workflow |
| 3 | Scheduling Engine | `SchedulerEngineService` | Poll PENDING jobs; match to AVAILABLE nodes; create executions |
| 4 | Log Streaming | `LogController` + `LogAggregator` | HTTP poll + SSE stream of stdout/stderr captured from workers |

---

## 2. Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Build & Run

```bash
git clone https://github.com/your-org/j-orchestrator.git
cd j-orchestrator
mvn clean package -DskipTests
java -jar target/j-orchestrator-1.0.0.jar
```

The application starts on **http://localhost:8080**.  
The H2 console is available at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:jorchestrator`).

Two worker nodes (`worker-node-1`, `worker-node-2`) are seeded automatically on startup.

### Minimal walkthrough

```bash
# 1. Submit a JAR job
curl -s -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "nightly-etl",
    "jobType": "JAR",
    "jarFilePath": "/jobs/etl-pipeline.jar",
    "mainClass": "com.example.EtlPipeline",
    "resourceConstraints": {
      "requiredCpuCores": 2,
      "requiredMemoryMb": 512,
      "maxExecutionSeconds": 300
    }
  }'

# 2. Watch it move through PENDING → SCHEDULED → RUNNING → COMPLETED
curl http://localhost:8080/api/jobs

# 3. Stream execution logs (replace <executionId> from the job response)
curl http://localhost:8080/api/logs/<executionId>

# 4. Cluster health (Facade)
curl http://localhost:8080/api/monitor/health
```

---

## 3. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  REST Layer (Controllers)                                                    │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐  ┌─────────────────┐ │
│  │JobController│  │NodeController │  │LogController │  │MonitorController│ │
│  └──────┬──────┘  └───────┬───────┘  └──────┬───────┘  └────────┬────────┘ │
└─────────┼─────────────────┼─────────────────┼──────────────────┼───────────┘
          │                 │                 │                  │ (Facade)
┌─────────▼─────────────────▼─────────────────▼──────────────────▼───────────┐
│  Service Layer                                                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │JobSubmissionSvc  │  │NodeRegistrySvc   │  │SchedulerEngineService    │  │
│  │JobValidationSvc  │  │(Observer Subject)│  │(Observer + Strategy Ctx) │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
│           │                     │  notifyOffline()         │ selectNode()   │
│     ┌─────▼───────┐             │         ┌───────────────▼──────────────┐ │
│     │ JobFactory  │             │         │  SchedulingStrategy (iface)  │ │
│     │  Registry   │             │         │  ├── RoundRobinStrategy      │ │
│     │  JarFact.   │             │         │  └── LeastLoadStrategy       │ │
│     │  ScriptFact.│             │         └──────────────────────────────┘ │
│     └─────────────┘             │                                           │
│  ┌──────────────────────────────▼────────────────────────────────────────┐  │
│  │  GRASP Pure Fabrications                                               │  │
│  │  LogAggregator (Module 4)    SystemMonitorFacade (GoF Facade)         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
          │                             │
┌─────────▼─────────────────────────────▼───────────────────────────────────┐
│  Repository Layer  (DIP — Services depend on interfaces only)              │
│  JobRepository  JobExecutionRepository  WorkerNodeRepository  LogEntryRepo │
└────────────────────────────────────────────────────────────────────────────┘
          │
┌─────────▼──────────────────────────────────────────────────────────────────┐
│  Domain Model                                                               │
│  Job (abstract) ──<Composition>── ResourceConstraints                      │
│    ├── JarJob                                                               │
│    └── ScriptJob                                                            │
│  JobExecution ──<Composition(job)>── Job                                   │
│              ──<Aggregation(node)>── WorkerNode                            │
│  WorkerNode  (GRASP Info Expert — owns isHealthy())                        │
│  LogEntry                                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. OOAD Concept → Class Mapping

### 4.1 SOLID Principles

#### S — Single Responsibility Principle

| Responsibility | Class | What it does NOT do |
|---|---|---|
| Job validation | `JobValidationService` | Does not persist, does not schedule |
| Job persistence + submission workflow | `JobSubmissionService` | Does not validate, does not run jobs |
| Scheduling (dispatch + eviction) | `SchedulerEngineService` | Does not validate, does not stream logs |
| Log retrieval | `LogAggregator` | Does not manage nodes or jobs |
| Node heartbeat tracking | `NodeRegistryService` | Does not schedule, does not submit jobs |
| Execution simulation | `WorkerSimulatorService` | Does not route HTTP, does not schedule |

> **Key SRP split:** `JobValidationService` and `JobSubmissionService` are deliberately separate.
> Validation rules (e.g., interpreter whitelist, `.jar` extension check) change independently
> from the submission workflow (factory selection, persistence). Merging them would create
> two reasons to change.

#### O — Open/Closed Principle

| Extension Point | Interface / Abstract Class | New type = new file only |
|---|---|---|
| New job type (e.g., Docker) | `JobFactory` (abstract) | Add `DockerJobFactory extends JobFactory`, register in `JobFactoryRegistry` |
| New scheduling algorithm | `SchedulingStrategy` (interface) | Add `WeightedRandomStrategy implements SchedulingStrategy`, annotate `@Component("WEIGHTED_RANDOM")` |
| New node-offline observer | `NodeEventListener` (interface) | Add `AlertNotificationService implements NodeEventListener`, call `nodeRegistryService.registerListener(...)` |

No existing class is modified when these extensions are made.

#### L — Liskov Substitution Principle

`JarJob` and `ScriptJob` are substitutable for `Job` everywhere in the system.
`SchedulerEngineService.buildCandidates()` works with `Job` (the parent type),
and `JobRepository.findByStatus()` returns `List<Job>` with mixed subtypes — the scheduler
never needs to downcast.

#### I — Interface Segregation Principle

| Interface | Size | Used by |
|---|---|---|
| `SchedulingStrategy` | 1 method | `SchedulerEngineService` |
| `NodeEventListener` | 1 method | `SchedulerEngineService` |
| `NodeEventPublisher` | 2 methods | `NodeRegistryService` implements it |
| `JobRepository` | `findByStatus`, `countByStatus` (+ JPA base) | `JobSubmissionService`, `SchedulerEngineService`, `SystemMonitorFacade` |

No client is forced to depend on methods it does not use.

#### D — Dependency Inversion Principle

Every service class declares its dependencies as **interfaces**, never as concrete Spring Data classes:

```java
// ✅ Correct — depends on the abstraction
private final JobRepository jobRepository;

// ❌ Wrong — would depend on the concrete implementation
private final SimpleJpaRepository<Job, UUID> jobRepository;
```

| High-level module | Depends on (abstraction) | Concrete class (Spring-generated) |
|---|---|---|
| `JobSubmissionService` | `JobRepository` | `SimpleJpaRepository<Job,UUID>` |
| `NodeRegistryService` | `WorkerNodeRepository` | `SimpleJpaRepository<WorkerNode,UUID>` |
| `SchedulerEngineService` | `SchedulingStrategy` | `RoundRobinStrategy` or `LeastLoadStrategy` |
| `SchedulerEngineService` | `JobRepository`, `JobExecutionRepository`, `WorkerNodeRepository` | Spring Data impls |

---

### 4.2 GRASP Patterns

#### Controller

The **GRASP Controller** pattern designates the first object beyond the UI that handles
a system event. In J-Orchestrator each REST controller is a GRASP Controller for its module:

| GRASP Controller | Class | System Event it Handles |
|---|---|---|
| Job Submission | `JobController` | DevOps Engineer submits / cancels a job |
| Node Management | `NodeController` | Worker Agent registers; Admin approves; heartbeat ping |
| Log Streaming | `LogController` | DevOps Engineer requests logs for an execution |
| Cluster Monitor | `MonitorController` | Any actor requests cluster health or log view |

Controllers contain **zero business logic**. They delegate entirely to the service layer.

#### Information Expert

The **Information Expert** principle: assign responsibility to the class that has the
information needed to fulfil it.

| Question | Information Owner | Method |
|---|---|---|
| "Is this node still alive?" | `WorkerNode` — holds `lastHeartbeat` | `WorkerNode.isHealthy(int timeoutSeconds)` |
| "Is this node available for new work?" | `WorkerNode` — holds `status` and `lastHeartbeat` | `WorkerNode.isAvailable(int timeoutSeconds)` |

`NodeRegistryService` calls `node.isHealthy(...)` rather than re-implementing the timestamp
comparison. This keeps the liveness logic co-located with the data it uses.

#### Pure Fabrication

Classes that exist purely to achieve good design (low coupling, high cohesion) with no
counterpart in the problem domain:

| Class | Why it's a Pure Fabrication |
|---|---|
| `LogAggregator` | No domain concept called "log aggregator" exists in the spec. Created to encapsulate log-retrieval query logic and decouple it from both the controller and the facade. |
| `JobValidationService` | No domain concept. Exists solely to isolate validation rules from the submission workflow (SRP). |
| `JobFactoryRegistry` | No domain concept. Exists solely to decouple job-type routing from the factory hierarchy (OCP). |
| `SchedulingStrategyFactory` | No domain concept. Exists solely to resolve the active strategy from configuration without an if/else chain. |

---

### 4.3 GoF Design Patterns

#### Factory Method

> *Define an interface for creating an object, but let subclasses decide which class to instantiate.*

| Role | Class |
|---|---|
| Abstract Creator | `JobFactory` (abstract class with `createJob()` factory method) |
| Concrete Creator — JAR | `JarJobFactory extends JobFactory` |
| Concrete Creator — Script | `ScriptJobFactory extends JobFactory` |
| Product (abstract) | `Job` |
| Concrete Product — JAR | `JarJob extends Job` |
| Concrete Product — Script | `ScriptJob extends Job` |
| Creator Registry | `JobFactoryRegistry` — maps `"JAR"` / `"SCRIPT"` to the right factory |
| Client | `JobSubmissionService.submit()` — calls `factory.build(request)` |

The client (`JobSubmissionService`) never writes `new JarJob(...)`. It calls:

```java
JobFactory factory = factoryRegistry.getFactory(request.getJobType()).orElseThrow();
Job job = factory.build(request);          // factory method call
```

#### Observer

> *Define a one-to-many dependency so that when one object changes state, all its dependents are notified automatically.*

| Role | Class |
|---|---|
| Subject interface | `NodeEventPublisher` |
| Concrete Subject | `NodeRegistryService implements NodeEventPublisher` |
| Observer interface | `NodeEventListener` |
| Concrete Observer | `SchedulerEngineService implements NodeEventListener` |

**Wiring:**
```java
// In SchedulerEngineService — registers on startup
@PostConstruct
public void init() {
    nodeRegistryService.registerListener(this);
}

// In NodeRegistryService — fires when heartbeat times out
private void notifyOffline(WorkerNode node) {
    listeners.forEach(l -> l.onNodeOffline(node));
}

// In SchedulerEngineService — the handler
@Override
public void onNodeOffline(WorkerNode node) {
    // evict running executions on that node, re-queue their jobs as PENDING
}
```

The Subject (`NodeRegistryService`) does not know about or import the Scheduler —
it only knows the `NodeEventListener` interface.

#### Strategy

> *Define a family of algorithms, encapsulate each one, and make them interchangeable.*

| Role | Class |
|---|---|
| Strategy interface | `SchedulingStrategy` |
| Concrete Strategy A | `RoundRobinStrategy` — cycles nodes via `AtomicInteger` |
| Concrete Strategy B | `LeastLoadStrategy` — picks node with min `currentLoad` |
| Context | `SchedulerEngineService` — calls `strategy.selectNode(job, candidates)` |
| Strategy resolver | `SchedulingStrategyFactory` — reads `jorchestrator.scheduler.strategy` from config |

Switch algorithms without restarting by updating `application.yml`:
```yaml
jorchestrator:
  scheduler:
    strategy: LEAST_LOAD   # or ROUND_ROBIN
```

#### Facade

> *Provide a unified interface to a set of interfaces in a subsystem.*

| Role | Class |
|---|---|
| Facade | `SystemMonitorFacade` |
| Subsystem A | `NodeRegistryService` |
| Subsystem B | `JobRepository` |
| Subsystem C | `JobExecutionRepository` |
| Subsystem D | `LogAggregator` |
| Client | `MonitorController` |

Without the facade, `MonitorController` would need to inject four separate beans,
understand their query conventions, and assemble the `ClusterHealthSnapshot` itself.
The facade collapses this into a single `getClusterHealth()` call.

---

### 4.4 OO Relationships

#### Composition (`Job` ◆——— `ResourceConstraints`)

`ResourceConstraints` has **no lifecycle independent of** `Job`. When a `Job` row is deleted,
its `ResourceConstraints` columns are deleted too — they live in the same table.

```java
// In Job.java
@Embedded                          // No separate table — true composition
private ResourceConstraints resourceConstraints;
```

```java
// In ResourceConstraints.java
@Embeddable                        // Not an @Entity — no independent identity
public class ResourceConstraints { ... }
```

#### Composition (`Job` ◆——— `JobExecution`)

A `JobExecution` **cannot exist without a parent Job**. It is created by the scheduler
at assignment time and would be meaningless without its owning job.

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)   // optional=false = mandatory owner
@JoinColumn(name = "job_id", nullable = false)
private Job job;
```

#### Aggregation (`JobExecution` ◇——— `WorkerNode`)

A `WorkerNode` **outlives any individual execution**. It has its own identity, its own
registration lifecycle, and can be referenced by many executions over time.

```java
@ManyToOne(fetch = FetchType.LAZY)   // FK reference only — node is independent
@JoinColumn(name = "node_id")        // nullable: can be nulled if node evicted
private WorkerNode assignedNode;
```

#### Inheritance (Factory Method Product hierarchy)

```
Job  (abstract, @Entity, SINGLE_TABLE inheritance)
 ├── JarJob    (@DiscriminatorValue("JAR"))
 └── ScriptJob (@DiscriminatorValue("SCRIPT"))
```

All rows live in the `jobs` table. The `job_type` discriminator column selects the subtype at load time.

---

## 5. Module Breakdown

### Module 1 — Job Submission (`JobController`)

```
POST /api/jobs   →   JobController.submit()
                         │
                         ▼
                 JobValidationService.validate()      (SRP: validation only)
                         │
                         ▼
                 JobFactoryRegistry.getFactory()       (OCP: open to new types)
                         │
                         ▼
                 JobFactory.build()                    (Factory Method)
                   ├── JarJobFactory     → JarJob
                   └── ScriptJobFactory  → ScriptJob
                         │
                         ▼
                 JobRepository.save()                  (DIP: interface)
```

### Module 2 — Node Management (`NodeRegistryService`)

```
@Scheduled (every 10 s)
    NodeRegistryService.checkHeartbeats()
        │
        for each AVAILABLE/BUSY node:
            node.isHealthy(30s)           ← GRASP Information Expert
            │
            if stale:
                node.status = OFFLINE
                notifyOffline(node)       ← Observer Subject fires event
                    │
                    └──▶ SchedulerEngineService.onNodeOffline()   ← Observer
                              evict RUNNING executions
                              re-queue jobs as PENDING
```

### Module 3 — Scheduling Engine (`SchedulerEngineService`)

```
@Scheduled (every 5 s)
    SchedulerEngineService.dispatchPendingJobs()
        │
        findByStatus(PENDING)
        │
        for each job:
            buildCandidates()   ← filter by CPU/RAM/health
            │
            strategy.selectNode()   ← Strategy Pattern
              ├── RoundRobinStrategy
              └── LeastLoadStrategy
                │
                assign(job, node)
                  ├── create JobExecution (Composition)
                  ├── job.status = SCHEDULED
                  └── node.currentLoad++
```

### Module 4 — Log Streaming (`LogController` + `LogAggregator`)

```
GET /api/logs/{id}          →  LogAggregator.getAllLogs()         HTTP poll
GET /api/logs/{id}/tail     →  LogAggregator.tailLogs()           HTTP poll  
GET /api/logs/{id}/sse      →  SseEmitter + polling loop          SSE stream
GET /api/monitor/…/logs     →  SystemMonitorFacade → LogAggregator  Facade
```

`LogAggregator` is a **GRASP Pure Fabrication**: no domain concept maps to "log aggregator".
It exists to keep query logic out of both the controller and the facade.

---

## 6. API Reference

### Jobs — `POST /api/jobs`

```json
{
  "name": "weekly-report",
  "jobType": "JAR",
  "jarFilePath": "/data/jobs/report.jar",
  "mainClass": "com.acme.ReportRunner",
  "jvmArgs": "-Xmx512m",
  "resourceConstraints": {
    "requiredCpuCores": 2,
    "requiredMemoryMb": 1024,
    "maxExecutionSeconds": 600
  }
}
```

```json
{
  "name": "data-cleanup",
  "jobType": "SCRIPT",
  "scriptContent": "#!/bin/bash\nfind /tmp -mtime +7 -delete",
  "interpreter": "bash",
  "resourceConstraints": {
    "requiredCpuCores": 1,
    "requiredMemoryMb": 128,
    "maxExecutionSeconds": 60
  }
}
```

| Method | Path | Actor | Description |
|---|---|---|---|
| `POST` | `/api/jobs` | DevOps | Submit a new JAR or Script job |
| `GET` | `/api/jobs` | DevOps | List all jobs |
| `GET` | `/api/jobs/{id}` | DevOps | Get job by ID |
| `DELETE` | `/api/jobs/{id}` | DevOps | Cancel a running or pending job |

### Nodes — `POST /api/nodes`

| Method | Path | Actor | Description |
|---|---|---|---|
| `POST` | `/api/nodes` | Worker Agent | Register a new node (starts PENDING_APPROVAL) |
| `POST` | `/api/nodes/{id}/approve` | Cluster Admin | Approve a pending node |
| `POST` | `/api/nodes/{id}/heartbeat` | Worker Agent | Send a heartbeat ping |
| `GET` | `/api/nodes` | Cluster Admin | List all registered nodes |

### Logs — `GET /api/logs/{executionId}`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/logs/{id}` | All log lines (chronological) |
| `GET` | `/api/logs/{id}/tail?lines=50` | Last N lines |
| `GET` | `/api/logs/{id}/stream/STDOUT` | Filter by stream |
| `GET` | `/api/logs/{id}/sse` | Server-Sent Events push stream |

### Monitor (Facade) — `GET /api/monitor/health`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/monitor/health` | Single-call cluster health snapshot |
| `GET` | `/api/monitor/executions/{id}/logs` | Full log dump via facade |
| `GET` | `/api/monitor/executions/{id}/logs/tail?n=50` | Tail via facade |

---

## 7. Configuration

All settings are in `src/main/resources/application.yml`.

```yaml
jorchestrator:
  scheduler:
    strategy: ROUND_ROBIN       # ROUND_ROBIN | LEAST_LOAD
    poll-interval-ms: 5000      # how often the scheduler polls for PENDING jobs

  node-registry:
    heartbeat-timeout-seconds: 30   # node marked OFFLINE after this silence
    check-interval-ms: 10000        # how often the heartbeat checker runs

  worker-simulator:
    enabled: true                   # disable to use real worker agents
    tick-interval-ms: 3000          # simulation advancement speed
```

---

## 8. Project Structure

```
src/main/java/com/jorchestrator/
│
├── JOrchestratorApplication.java          Entry point (@EnableScheduling)
│
├── config/
│   ├── DataInitializer.java               Seeds 2 worker nodes on startup
│   └── GlobalExceptionHandler.java        @RestControllerAdvice — 400/409/500 shaping
│
├── controller/                            GRASP Controller layer
│   ├── JobController.java                 Module 1 — job submission REST API
│   ├── NodeController.java                Module 2 — node management REST API
│   ├── LogController.java                 Module 4 — log polling + SSE endpoint
│   └── MonitorController.java             Facade client — single-call monitor API
│
├── dto/
│   └── JobSubmissionRequest.java          Inbound job payload + ResourceConstraintsDto
│
├── model/
│   ├── job/
│   │   ├── Job.java                       Abstract product — Factory Method root
│   │   ├── JarJob.java                    Concrete product — JAR execution
│   │   ├── ScriptJob.java                 Concrete product — script execution
│   │   ├── ResourceConstraints.java       @Embeddable — Composition with Job
│   │   ├── JobExecution.java              Composition(Job) + Aggregation(WorkerNode)
│   │   ├── JobStatus.java                 PENDING → SCHEDULED → RUNNING → COMPLETED/FAILED/CANCELLED
│   │   └── ExecutionStatus.java           ASSIGNED → RUNNING → COMPLETED/FAILED/EVICTED
│   ├── node/
│   │   ├── WorkerNode.java                GRASP Information Expert (isHealthy)
│   │   └── NodeStatus.java                AVAILABLE | BUSY | OFFLINE | PENDING_APPROVAL
│   └── log/
│       └── LogEntry.java                  Captured stdout/stderr line
│
├── repository/                            DIP — services depend on these interfaces
│   ├── JobRepository.java
│   ├── JobExecutionRepository.java
│   ├── WorkerNodeRepository.java
│   └── LogEntryRepository.java
│
├── factory/                               Factory Method pattern
│   ├── JobFactory.java                    Abstract Creator + template method
│   ├── JarJobFactory.java                 Concrete Creator → JarJob
│   ├── ScriptJobFactory.java              Concrete Creator → ScriptJob
│   └── JobFactoryRegistry.java            "JAR"/"SCRIPT" → factory lookup (OCP)
│
├── scheduling/                            Strategy pattern
│   ├── SchedulingStrategy.java            Strategy interface (OCP extension point)
│   ├── RoundRobinStrategy.java            Concrete Strategy A
│   ├── LeastLoadStrategy.java             Concrete Strategy B
│   └── SchedulingStrategyFactory.java     Resolves active strategy from config
│
├── observer/                              Observer pattern interfaces
│   ├── NodeEventPublisher.java            Subject interface
│   └── NodeEventListener.java             Observer interface
│
├── aggregator/
│   └── LogAggregator.java                 GRASP Pure Fabrication — log query logic
│
├── facade/
│   └── SystemMonitorFacade.java           GoF Facade — single UI entry point
│
└── service/
    ├── JobValidationService.java           SRP — validation rules only
    ├── JobSubmissionService.java           SRP — submit/cancel workflow
    ├── NodeRegistryService.java            Observer Subject + heartbeat scheduler
    ├── SchedulerEngineService.java         Observer Observer + Strategy Context
    └── WorkerSimulatorService.java         Simulates remote Worker Agent
```