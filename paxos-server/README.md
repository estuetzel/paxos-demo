# Paxos Server

A Java Spring Boot implementation of a Paxos Acceptor node with both REST and gRPC interfaces.
There are two logical REST controllers/interfaces:
PaxosAcceptor -- a true http implementation of the Paxos acceptor protocol
PaxosBroadcast -- built for both:
* convenience - The contacted server broadcasts the request to other nodes.
* demonstration purposes - This is an interview exercise on showing knowledge of distributed
systems. This controller internally uses gRPC to communicate with other nodes, a very common
tool in distributed systems.
* note - the configuration of how each node knows how many and what other nodes to contact is 
greatly simplified from what one would expect in a robust production system

The acceptors read their state from an environment configured data file and
persist to this file after any prepare or accept operation so that state is preserved
through restarts or unexpected outage.

## Requirements/Setup

- Java 17+
- Maven 3.6+
```commandline
# assume java already install
brew install maven
# build and run tests
mvn clean package
```

## Running

Use the run-server script to launch a server with a specific ID. The pool size, or 
numServers, is required only for the features enabled by the Proposer controller. Pure
Paxos acceptors don't need to know how many acceptors are in the pool.

The serverID is in the sed [1,N] where N is numServers.

```bash
# choose a data dir for state persistence
export PAXOS_DATA_DIR=$(pwd)/data
./run-server.sh <serverID> <numServers>
```

Example - starting three servers, run each in its own terminal:
```bash
./run-server.sh 1 3 # HTTP: 8081, gRPC: 9091
./run-server.sh 2 3 # HTTP: 8082, gRPC: 9092
./run-server.sh 3 3 # HTTP: 8083, gRPC: 9093
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
  "promisedId": 8
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

These are for efficient + stable internode communication. They are for demonstration
purposes only since this is an interview exercise.

## Example Usage

```bash
# Start 3 servers, run each in its own terminal
./run-server.sh 1 3
./run-server.sh 2 3 
./run-server.sh 3 3 

# Test prepare 
curl -X POST "http://localhost:8081/api/paxos/prepare?id=1"

# Test accept
curl -X POST "http://localhost:8081/api/paxos/accept?id=1&value=hello"
```

## Testing
There is comprehensive testing of the PaxosAcceptorService as getting
this part correct is most critical to the Paxos protocol.

There is good e2e integration test coverage of the acceptor rest interface as well.

There is just light testing of the proposer/controller and grpc code since these
don't affect the correctness of the Paxos Acceptor behavior of the server and these
were added for demonstration purposes. The multi-server integration test is very useful
for catching bugs that would never be caught in a unit test.

## Future work

### Containerize
Right now multiple servers just run on localhost using incremental port numbers
for http and gRPC. This is not ideal. For a production deployment and improved local 
dev experience, running each acceptor node as a docker container with docker networking
would be much better. 

# Metrics
Add metrics for request time processing, request count, etc

### Internode Communication Monitoring
Add a periodic background process that uses the gRPC ping endpoint to monitor the
health of internode communication. If a node is unable to contact another node, a
warning should be logged and (in the future) metrics updated.
