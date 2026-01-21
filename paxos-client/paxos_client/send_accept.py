#!/usr/bin/env python3

import argparse

from utils import send_accept


def main():
    parser = argparse.ArgumentParser(description="Paxos send accept to specified servers")
    parser.add_argument("--server-ids", nargs='+', required=True)
    parser.add_argument("--id", type=int, required=True)
    parser.add_argument("--value", type=str, required=True)

    args = parser.parse_args()

    server_ids = [int(s) for s in args.server_ids]

    print(f"\nContacting servers: {server_ids}")
    print(f"ID: {args.id}")
    print(f"Client value: {args.value}")

    # -------------------------
    # Send prepares
    # -------------------------
    accept_responses = []

    for sid in server_ids:
        resp = send_accept(sid, args.id, args.value)
        if resp is None:
            print(f"[prepare] server {sid}: no response")
            continue
        print(f"[prepare] server {sid}: {resp}")
        accept_responses.append((sid, resp))

    accepted_servers = [
        sid
        for sid, r in accept_responses
        if r.get("ignored") is False
    ]
    print(f"Received accepts from: {accepted_servers}")


if __name__ == "__main__":
    main()
