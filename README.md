# Distributed File System (DFS)

A fault-tolerant distributed key-value storage system built in Java that demonstrates core distributed systems concepts including **Consistent Hashing**, **Replication**, **Heartbeat-based Failure Detection**, **Automatic Node Discovery**, and **Data Rebalancing**.

The project is designed as a learning implementation of the core building blocks used in systems such as Amazon Dynamo, Cassandra, and Riak.

---

## Features

- Consistent Hashing with 100 Virtual Nodes per physical node
- TCP-based distributed file transfer
- UDP-based node discovery and cluster membership
- Heartbeat-based failure detection
- Automatic data rebalancing on topology changes
- Configurable replication factor (default RF = 3)
- Thread-safe storage using Read/Write locks
- Interactive CLI
- Graceful node shutdown

---

## Architecture

```text
                        +-------------------+
                        |      CLI          |
                        +---------+---------+
                                  |
                                  v
                  +---------------+----------------+
                  | DistributedStorageService      |
                  +---------------+----------------+
                                  |
                    +-------------+-------------+
                    |                           |
                    v                           v
          ConsistentHashRing         ReplicationManager
                    |                           |
                    |                           v
                    |                  TcpRequestClient
                    |                           |
                    +-------------+-------------+
                                  |
                                  v
                        TcpRequestServer
                                  |
                                  v
                         StorageManager
                                  |
                                  v
                         LocalDiskService
                                  |
                                  v
                        FileLockManager
```

---

## Cluster Components

| Component | Responsibility |
|-----------|----------------|
| ConsistentHashRing | Determines file ownership |
| ClusterManager | Maintains cluster topology |
| UdpMembershipBroadcaster | Broadcasts JOIN / LEAVE / HEARTBEAT |
| UdpMembershipListener | Updates cluster membership |
| HeartbeatManager | Sends periodic heartbeats |
| FailureDetector | Detects failed nodes |
| ReplicationManager | Replicates files to backup nodes |
| RebalancingManager | Migrates files after topology changes |
| StorageManager | Thread-safe storage operations |
| LocalDiskService | Physical disk I/O |

---

## Tech Stack

- Java 17
- Java NIO
- TCP Sockets
- UDP Multicast
- Concurrent Collections
- ReentrantReadWriteLock
- JUnit 5
- Mockito

---

## Project Structure

```text
src/
├── client/
├── cluster/
├── common/
├── hashing/
├── membership/
├── network/
├── rebalance/
├── replication/
├── storage/
└── launcher/
```

---

## Running the Project

### Compile

```bash
javac -d out $(find src -name "*.java")
```

### Start Node 1

```bash
java -cp out launcher.Main 8081 9999
```

### Start Node 2

```bash
java -cp out launcher.Main 8082 9999
```

### Start Node 3

```bash
java -cp out launcher.Main 8083 9999
```

---

## CLI Commands

```text
PUT <key> <data>
GET <key>
DELETE <key>
EXISTS <key>
HELP
EXIT
```

Example:

```text
dfs> PUT resume "Hello World"

dfs> GET resume

Hello World
```

---

## Distributed Workflow

### PUT

```text
CLI
    ↓
DistributedStorageService
    ↓
ConsistentHashRing
    ↓
Primary Owner
    ↓
StorageManager
    ↓
ReplicationManager
```

---

### GET

```text
CLI
    ↓
DistributedStorageService
    ↓
Primary Owner
    ↓
StorageManager
```

---

### Failure Detection

```text
Heartbeat
      ↓
FailureDetector
      ↓
ClusterManager
      ↓
ConsistentHashRing
      ↓
RebalancingManager
```

---

## Unit Tests

### Run all tests

```bash
java -cp "lib/*:out" org.junit.platform.console.ConsoleLauncher --scan-classpath
```

## Current Limitations

- Files are transferred as byte arrays (streaming planned)
- No TLS encryption
- No authentication
- Local disk only
- No persistence of cluster metadata
- UDP multicast is intended for local-network deployments

---

## Future Enhancements

- Chunked streaming for large files
- Asynchronous replication
- Erasure Coding
- Vector Clocks
- Versioning
- Gossip protocol improvements
- Metrics endpoint
- Docker Compose deployment
- Kubernetes deployment
- Web UI

---

## What I Learned

This project helped me understand:

- Consistent Hashing
- Distributed file placement
- Replication strategies
- Failure detection
- Data rebalancing
- Concurrent programming
- Socket programming
- Distributed systems fundamentals

---
