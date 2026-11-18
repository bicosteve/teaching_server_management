"""CLI entrypoint for the Teaching Server Management API client."""

from __future__ import annotations

import argparse

from config import DEFAULT_BASE_URL
from ui.app import run_ui


def main() -> int:
	parser = argparse.ArgumentParser(
		description="TSM Server Operations Client",
	)
	parser.add_argument(
		"--base-url",
		default=DEFAULT_BASE_URL,
		help="Server base URL (default: %(default)s)",
	)
	parser.add_argument("--username", help="Username or email (skip prompt)")
	args = parser.parse_args()
	return run_ui(args.base_url, username=args.username)


if __name__ == "__main__":
	raise SystemExit(main())
