# Paxos Server

A Spring Boot implementation of a Paxos consensus node with both REST and gRPC interfaces.

## Requirements

- Java 17+
- Maven 3.6+

## Building

```bash
mvn clean package
```

## Running

Use the provided script to launch a server with a specific ID:

```bash
./run-server.sh <serverID>
```

Example - starting three servers:
```bash
./run-server.sh 1  # HTTP: 8081, gRPC: 9091
./run-server.sh 2  # HTTP: 8082, gRPC: 9092
./run-server.sh 3  # HTTP: 8083, gRPC: 9093
```

## REST API Endpoints

Base URL: `http://localhost:<8080+serverID>/api/paxos`

### Prepare (Phase 1)

```bash
POST /api/paxos/prepare?id=<proposalId>
```

Response:
```json
{
  "ignored": false,
  "id": 8
}
```

### Accept (Phase 2)

```bash
POST /api/paxos/accept?id=<proposalId>&value=<value>
```

Response:
```json
{
  "ignored": false,
  "acceptedId": 1,
  "acceptedValue": "myValue"
}
```

### Health Check

```bash
GET /api/paxos/health
```

## gRPC Endpoints

not used yet

## Example Usage

```bash
# Start server 1
./run-server.sh 1

# Test prepare (in another terminal)
curl -X POST "http://localhost:8081/api/paxos/prepare?id=1"

# Test accept
curl -X POST "http://localhost:8081/api/paxos/accept?id=1&value=hello"

# Health check
curl http://localhost:8081/api/paxos/health
```

## Project Structure
 
TODO
