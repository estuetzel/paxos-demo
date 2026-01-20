#!/bin/bash

# Paxos Server Launch Script
# Usage: ./run-server.sh <serverID>
# Example: ./run-server.sh 1

if [ -z "$1" ]; then
    echo "Usage: $0 <serverID> <numServers>"
    echo "Example: $0 1 3"
    exit 1
fi
# TODO make this validation more concise
if [ -z "$2" ]; then
    echo "Usage: $0 <serverID> <numServers>"
    echo "Example: $0 1 3"
    exit 1
fi

SERVER_ID=$1
NUM_SERVERS=$2

# Calculate ports based on server ID
# HTTP port: 8080 + serverID
# gRPC port: 9090 + serverID
HTTP_PORT=$((8080 + SERVER_ID))
GRPC_PORT=$((9090 + SERVER_ID))

echo "Starting Paxos Server ${SERVER_ID}"
echo "  HTTP Port: ${HTTP_PORT}"
echo "  gRPC Port: ${GRPC_PORT}"
echo ""

# Change to script directory
cd "$(dirname "$0")"

# Build if necessary
if [ ! -f "target/paxos-server-1.0.0-SNAPSHOT.jar" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Run the server
java -jar target/paxos-server-1.0.0-SNAPSHOT.jar \
    --paxos.server.id=${SERVER_ID} \
    --paxos.server.count=${NUM_SERVERS} \
    --server.port=${HTTP_PORT} \
    --grpc.server.port=${GRPC_PORT}
