#!/usr/bin/env python
"""Wait until the local HAPI FHIR endpoint returns a CapabilityStatement."""

from __future__ import annotations

import json
import http.client
import os
import sys
import time
import urllib.error
import urllib.request


FHIR_BASE_URL = os.getenv("FHIR_BASE_URL", "http://localhost:8080/fhir").rstrip("/")
TIMEOUT_SECONDS = int(os.getenv("HAPI_WAIT_TIMEOUT_SECONDS", "180"))
POLL_SECONDS = int(os.getenv("HAPI_WAIT_POLL_SECONDS", "5"))


def get_metadata() -> dict:
    request = urllib.request.Request(
        f"{FHIR_BASE_URL}/metadata",
        headers={"Accept": "application/fhir+json"},
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    deadline = time.monotonic() + TIMEOUT_SECONDS
    last_error = ""

    while time.monotonic() < deadline:
        try:
            metadata = get_metadata()
            if metadata.get("resourceType") == "CapabilityStatement":
                software = metadata.get("software", {}).get("name", "HAPI FHIR")
                print(f"Ready: {software} is serving {FHIR_BASE_URL}")
                return 0
            last_error = f"unexpected resourceType={metadata.get('resourceType')!r}"
        except (
            urllib.error.URLError,
            TimeoutError,
            ConnectionError,
            OSError,
            json.JSONDecodeError,
            http.client.RemoteDisconnected,
        ) as exc:
            last_error = str(exc)

        print(f"Waiting for HAPI FHIR at {FHIR_BASE_URL} ({last_error})")
        time.sleep(POLL_SECONDS)

    print(f"Timed out waiting for HAPI FHIR at {FHIR_BASE_URL}: {last_error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
