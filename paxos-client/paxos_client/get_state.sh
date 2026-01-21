#!/bin/bash
# this uses the beta proposer controller to get the state of all servers
curl --silent localhost:8081/api/proposer/paxos/state | jq .