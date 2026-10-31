# MDP — Distributed Invoice Processing System

## Overview

A system for automatic processing of XML invoices. It consists of 10 independent Java programs that communicate through different network protocols. Each invoice passes through several phases: reception, parsing, validation, enrichment (currency exchange rate), approval and aggregation, and is finally shown in GUI clients and through a REST API.

## Modules

| Module | Role | Communication |
|---|---|---|
| `common` | shared models, RMI interface, utilities, configuration | — |
| `watcher` | monitors the `inbox` folder and sends files to Parser | TCP socket |
| `parser` | receives invoices, verifies hash, parses XML, checks duplicates | TCP server, RMI client, Redis, MQ |
| `validator` | validates invoices by 6 rules | RMI server |
| `enrichment` | exchange rate (REST API + Redis cache), computes `totalBAM` | MQ, REST, Redis |
| `approval` | automatic approval below threshold (100 BAM) | MQ |
| `approval-client` | GUI for manual approve/reject | MQ, Swing |
| `aggregator` | stores invoices in Redis, updates clients, sends notifications | MQ, Redis, UDP multicast |
| `monitor-client` | GUI showing approved invoices, can run in multiple instances | UDP multicast, Swing |
| `rest-api` | HTTP API over data stored in Redis | HTTP |

## Protocol (Watcher → Parser)

Each message is sent as follows:

| Part | Size | Content |
|---|---|---|
| file name length | 4 bytes | big-endian int |
| file name | M bytes | UTF-8 |
| XML length | 4 bytes | big-endian int |
| XML | N bytes | file content |
| hash | 32 bytes | SHA-256 of the XML |

Parser replies with an ACK containing the file name. The file name is included in the protocol because the ACK must contain it; the assignment allows this ("everything not precisely defined can be implemented as considered best").

## Running the system

1. Start Docker: `docker compose up -d` (Redis and ActiveMQ)
2. Start the services in this order:
   1. `ValidatorApp`
   2. `ParserApp`
   3. `EnrichmentApp`
   4. `ApprovalApp`
   5. `AggregatorApp`
   6. `ApprovalClientApp`
   7. `MonitorApp` (multiple instances possible with `-Dinstance.name=...`)
   8. `RestApiApp`
   9. `WatcherApp`
3. Copy files from `data/invoices/generated` into `data/invoices/inbox`.

All services read configuration from `common/src/main/resources/config.properties`. The working directory must be the project root (relative paths and logs depend on it).

## REST API

| Endpoint | Description |
|---|---|
| `GET /api/invoices` | list of all invoices |
| `GET /api/invoices/{id}` | single invoice |
| `GET /api/invoices?status={status}` | filter by status (approved, rejected, failed, pending) |
| `GET /api/clients/{jib}/summary` | total BAM, number of invoices, last invoice |
| `GET /api/stats` | total invoices, total BAM, average, number of pending |

## Notes

- `data/invoices/generated` contains 120 generated sample invoices.
- Logs are written to the `logs/` folder.
- Redis remembers invoice IDs for 7 days, so for a repeated test run `FLUSHALL` first.
