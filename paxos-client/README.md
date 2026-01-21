## Paxos Client
This project contains several scripts for interacting with Paxos acceptors
as a proposer.

User must be aware of how many servers are in the acceptor pool to use most of 
these scripts

## setup
User must have already launched one or more acceptors in the paxos-server project.
User can bring acceptors up and down as wanted to introduce downed-node
behavior.
```commandline
python -m venv .venv 
source .venv/bin/activate 
pip install -r requirements.txt
```
Recommended:
```commandline
brew install jq
```
### get-state
Get-state simply contacts the first node in the acceptor pool, which then gets
the state of itself and all other nodes in the pool. This is a convenience script
and isn't part of the Paxos proposer protocol.
```commandline
./paxos_client/get_state.sh                                 
{
  "1": {
    "promisedId": 56,
    "acceptedId": 5,
    "acceptedValue": "asdf"
  },
  "2": {
    "promisedId": 56,
    "acceptedId": 5,
    "acceptedValue": "asdf"
  },
  "3": {
    "promisedId": 56,
    "acceptedId": 5,
    "acceptedValue": "asdf"
  }
}
```

### send-prepare
Send a prepare message to an explicit list of server ids in [1,N] where N is the pool size.
```commandline
python paxos_client/send_prepare.py --id 7 --server-ids 1 2                 

Contacting servers: [1, 2]
ID: 7
[prepare] server 1: {'ignored': False, 'promisedId': 7}
[prepare] server 2: {'ignored': False, 'promisedId': 7}
Received promise from: [1, 2]
```

### send-accept
Send an accept request message to an explicit list of server ids in [1,N] where N is the pool size.
```commandline
python paxos_client/send_accept.py --id 7 --server-ids 1 2 --value test-val 

Contacting servers: [1, 2]
ID: 7
Client value: test-val
[prepare] server 1: {'ignored': False, 'acceptedId': 7, 'acceptedValue': 'test-val'}
[prepare] server 2: {'ignored': False, 'acceptedId': 7, 'acceptedValue': 'test-val'}
Received accepts from: [1, 2]
```

### proposer-demo
This script follows the Paxos proposer protocol from beginning to end and prints out
what consensus value has been reached, if any.

You can use the contact count to play around with different majority or minority 
acceptor server contacts to understand how Paxos behaves. A random set of acceptors
are chosen each time contact_count is less than the pool size.
```commandline
python paxos_client/proposer_demo.py --help
usage: proposer_demo.py [-h] --server-count SERVER_COUNT --id ID --contact-count CONTACT_COUNT --value VALUE

Paxos proposer demo

options:
  -h, --help            show this help message and exit
  --server-count SERVER_COUNT
                        The number of acceptors in pool
  --id ID               The id to propose. Use in according w/ Paxos proposer protocol
  --contact-count CONTACT_COUNT
                        Contact a random subset of servers when this number is less than server count
  --value VALUE         The value to propose
```


Example consensus already reached
```commandline
python paxos_client/proposer_demo.py --server-count 3 --promise-id 9 --contact-count 3 --value BAD

Contacting servers: [3, 2, 1]
Proposal ID: 9
Client value: BAD
[prepare] server 3: {'ignored': False, 'promisedId': 9}
[prepare] server 2: {'ignored': False, 'promisedId': 9, 'acceptedId': 7, 'acceptedValue': 'test-val'}
[prepare] server 1: {'ignored': False, 'promisedId': 9, 'acceptedId': 7, 'acceptedValue': 'test-val'}

Client value overridden due to prior accepted value:
  acceptedId=7, value=test-val

Consensus already reached during prepare phase:
  acceptedId=7, value=test-val
```

Example consensus reached using proposed id and value:
```commandline
python paxos_client/proposer_demo.py --server-count 3 --promise-id 9 --contact-count 3 --value good-value

Contacting servers: [3, 1, 2]
Proposal ID: 9
Client value: good-value
[prepare] server 3: {'ignored': False, 'promisedId': 9}
[prepare] server 1: {'ignored': False, 'promisedId': 9}
[prepare] server 2: {'ignored': False, 'promisedId': 9}

Sending accept requests with value: good-value
[accept] server 3: {'ignored': False, 'acceptedId': 9, 'acceptedValue': 'good-value'}
[accept] server 1: {'ignored': False, 'acceptedId': 9, 'acceptedValue': 'good-value'}
[accept] server 2: {'ignored': False, 'acceptedId': 9, 'acceptedValue': 'good-value'}

--- Result ---
Accept attempts sent to servers: [3, 1, 2]
Accepts received from servers: [3, 1, 2]

Consensus Reached
  proposalId=9
  value=good-value
```