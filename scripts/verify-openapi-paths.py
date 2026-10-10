#!/usr/bin/env python3
"""Fail when a maintained OpenAPI file and the running service expose different paths."""

import json
import re
import sys
import urllib.request
from pathlib import Path


def documented_paths(contract: Path) -> set[str]:
    pattern = re.compile(r"^  (/[^:]+):\s*$")
    return {
        match.group(1)
        for line in contract.read_text(encoding="utf-8").splitlines()
        if (match := pattern.match(line))
    }


def runtime_paths(url: str) -> set[str]:
    with urllib.request.urlopen(url, timeout=10) as response:
        return set(json.load(response)["paths"])


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: verify-openapi-paths.py CONTRACT_YAML RUNTIME_OPENAPI_URL", file=sys.stderr)
        return 2
    contract = Path(sys.argv[1])
    documented = documented_paths(contract)
    runtime = runtime_paths(sys.argv[2])
    if documented == runtime:
        print(f"OpenAPI paths match for {contract}: {len(runtime)} paths")
        return 0
    print(f"OpenAPI path drift for {contract}", file=sys.stderr)
    if runtime - documented:
        print(f"  undocumented: {sorted(runtime - documented)}", file=sys.stderr)
    if documented - runtime:
        print(f"  not implemented: {sorted(documented - runtime)}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
