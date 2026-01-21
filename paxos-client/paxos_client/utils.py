import requests

TIMEOUT = 1.0  # seconds
BASE_PORT = 8081


def server_url(server_id: int) -> str:
    return f"http://localhost:{BASE_PORT + server_id - 1}"


def send_prepare(server_id: int, proposal_id: int):
    url = f"{server_url(server_id)}/api/paxos/prepare"
    try:
        r = requests.post(url, params={"id": proposal_id}, timeout=TIMEOUT)
        r.raise_for_status()
        return r.json()
    except Exception:
        return None


def send_accept(server_id: int, proposal_id: int, value: str):
    url = f"{server_url(server_id)}/api/paxos/accept"
    try:
        r = requests.post(
            url,
            params={"id": proposal_id, "value": value},
            timeout=TIMEOUT,
        )
        r.raise_for_status()
        return r.json()
    except Exception:
        return None
