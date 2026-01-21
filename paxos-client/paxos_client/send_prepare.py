#!/usr/bin/env python3

import argparse

from utils import send_prepare


def main():
    parser = argparse.ArgumentParser(description="Paxos send prepare to specified servers")
    parser.add_argument("--server-ids", nargs='+', required=True)
    parser.add_argument("--id", type=int, required=True)

    args = parser.parse_args()

    server_ids = [int(s) for s in args.server_ids]

    print(f"\nContacting servers: {server_ids}")
    print(f"ID: {args.id}")

    # -------------------------
    # Send prepares
    # -------------------------
    prepare_responses = []

    for sid in server_ids:
        resp = send_prepare(sid, args.id)
        if resp is None:
            print(f"[prepare] server {sid}: no response")
            continue
        print(f"[prepare] server {sid}: {resp}")
        prepare_responses.append((sid, resp))

    promised_servers = [
        sid
        for sid, r in prepare_responses
        if r.get("ignored") is False
    ]
    print(f"Received promise from: {promised_servers}")


if __name__ == "__main__":
    main()
