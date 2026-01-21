#!/usr/bin/env python3

import argparse
import random
from collections import Counter

from utils import send_accept, send_prepare


def majority(n: int) -> int:
    return (n // 2) + 1


def main():
    parser = argparse.ArgumentParser(description="Paxos proposer demo")
    parser.add_argument("--server-count", type=int, required=True, help="The number of acceptors in pool")
    parser.add_argument("--id", type=int, required=True,
                        help="The id to propose. Use in according w/ Paxos proposer protocol")
    parser.add_argument("--contact-count", type=int, required=True,
                        help="Contact a random subset of servers when this number is less than server count")
    parser.add_argument("--value", type=str, required=True, help="The value to propose")

    args = parser.parse_args()

    server_ids = list(range(1, args.server_count + 1))
    contacted = random.sample(
        server_ids, min(args.contact_count, len(server_ids))
    )

    print(f"\nContacting servers: {contacted}")
    print(f"Proposal ID: {args.id}")
    print(f"Client value: {args.value}")

    # Prepare Phase
    prepare_responses = []

    for sid in contacted:
        resp = send_prepare(sid, args.id)
        if resp is None:
            print(f"[prepare] server {sid}: no response")
            continue
        print(f"[prepare] server {sid}: {resp}")
        prepare_responses.append((sid, resp))

    promises = [
        (sid, r)
        for sid, r in prepare_responses
        if r.get("ignored") is False
    ]

    if not promises:
        print("\nNo promises received — proposal rejected everywhere.")
        return

    # Collect accepted values
    accepted_reports = [
        (r["acceptedId"], r["acceptedValue"])
        for _, r in promises
        if "acceptedId" in r and "acceptedValue" in r
    ]

    # Value Selection
    chosen_value = args.value

    if accepted_reports:
        # must use the value of the highest accepted id per paxos proposer protocol
        accepted_reports.sort(key=lambda x: x[0], reverse=True)
        highest_id, highest_value = accepted_reports[0]
        chosen_value = highest_value

        print(
            f"\nClient value overridden due to prior accepted value:"
            f"\n  acceptedId={highest_id}, value={highest_value}"
        )

    # Early decision detection
    value_counts = Counter(accepted_reports)
    for (aid, val), count in value_counts.items():
        if count >= majority(args.server_count):
            print(
                f"\nConsensus already reached during prepare phase:"
                f"\n  acceptedId={aid}, value={val}"
            )
            return

    # Accept Phase
    print(f"\nSending accept requests with value: {chosen_value}")

    accept_successes = []
    accept_attempts = []

    for sid, _ in promises:
        resp = send_accept(sid, args.id, chosen_value)
        accept_attempts.append(sid)

        if resp is None:
            print(f"[accept] server {sid}: no response")
            continue

        print(f"[accept] server {sid}: {resp}")

        if resp.get("ignored") is False:
            accept_successes.append(sid)

    # Final Outcome
    print("\n--- Result ---")
    print(f"Accept attempts sent to servers: {accept_attempts}")
    print(f"Accepts received from servers: {accept_successes}")

    if len(accept_successes) >= majority(args.server_count):
        print(
            f"\nConsensus Reached"
            f"\n  proposalId={args.id}"
            f"\n  value={chosen_value}"
        )
    else:
        print(
            f"\nNo Consensus"
            f"\n  accepted by {len(accept_successes)} servers"
            f"\n  majority required: {majority(args.server_count)}"
        )


if __name__ == "__main__":
    main()
