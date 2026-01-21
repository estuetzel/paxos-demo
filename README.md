# Paxos Protocol -- Interview Exercise
This project contains an implementation of Simple Paxos Protocol,
written for an interview exercise.

There are two subprojects

### paxos-server
A Java Spring Boot implementation of Paxos Acceptor role.
* The Acceptor has a simple http REST interface
* It also contains some supplemental behavior for the purpose of
demonstrating distributed system knowledge. 

### paxos-client

Various scripts for Paxos Proposer Role functionality. The proposer
role involves
* adhering to the Paxos proposer protocol
* sending requests to a majority (usually) ofAcceptors and interpreting the responses