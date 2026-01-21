# Paxos Protocol -- Interview Exercise
This project contains an implementation of Simple Paxos Protocol,
written for an interview exercise.

This implementation is based on reviewing the following sources:
* https://en.wikipedia.org/wiki/Paxos_(computer_science)
* [Google Youtube Paxos Presentation](https://www.youtube.com/watch?v=d7nAGI_NZPk)

There are two subprojects.

### [paxos-server](paxos-server/README.md)
A Java Spring Boot implementation of Paxos Acceptor role.
* The acceptor has a simple http REST interface
* It also contains some supplemental behavior for the purpose of
demonstrating distributed system knowledge. 

### [paxos-client](paxos-client/README.md)

Various python scripts for Paxos Proposer Role functionality. The proposer
role involves
* adhering to the Paxos proposer protocol
* sending requests to a majority (usually) of acceptors and interpreting the responses