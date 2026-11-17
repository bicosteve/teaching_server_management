"""Minimal http client for Teaching Server Management API."""

from __future__ import annotations

import argparse
import getpass
import json
import sys
from typing import Any

import httpx


class TsmClient:
	"""httpx client that logs in, stores JWTs, and calls the TSM API with Bearer auth."""

	def __init__(self, base_url: str, timeout: float = 10.0) -> None:
		self.base_url = base_url.rstrip("/")
		self._client = httpx.Client(base_url=self.base_url, timeout=timeout)
		self.access_token: str | None = None
		self.refresh_token: str | None = None
		self.user: dict[str, Any] | None = None

	def close(self) -> None:
		self._client.close()

	def __enter__(self) -> TsmClient:
		return self

	def __exit__(self, *args: object) -> None:
		self.close()

	def login(self, username_or_email: str, password: str) -> dict[str, Any]:
		response = self._client.post(
			"/api/auth/login",
			json={"usernameOrEmail": username_or_email, "password": password},
		)
		response.raise_for_status()
		payload = response.json()
		self._store_tokens(payload)
		return payload

	def refresh(self) -> dict[str, Any]:
		if not self.refresh_token:
			raise RuntimeError("No refresh token; call login() first")
		response = self._client.post(
			"/api/auth/refresh",
			json={"refreshToken": self.refresh_token},
		)
		response.raise_for_status()
		payload = response.json()
		self._store_tokens(payload)
		return payload

	def me(self) -> dict[str, Any]:
		return self.get("/api/auth/me")

	def get(self, path: str, **kwargs: Any) -> Any:
		return self._request("GET", path, **kwargs)

	def post(self, path: str, **kwargs: Any) -> Any:
		return self._request("POST", path, **kwargs)

	def _request(self, method: str, path: str, **kwargs: Any) -> Any:
		headers = dict(kwargs.pop("headers", {}) or {})
		if self.access_token:
			headers["Authorization"] = f"Bearer {self.access_token}"
		response = self._client.request(method, path, headers=headers, **kwargs)
		response.raise_for_status()
		if response.status_code == 204 or not response.content:
			return None
		return response.json()

	def _store_tokens(self, payload: dict[str, Any]) -> None:
		self.access_token = payload["accessToken"]
		self.refresh_token = payload["refreshToken"]
		self.user = payload.get("user")


def main() -> int:
	parser = argparse.ArgumentParser(description="Authenticate to the TSM Spring API")
	parser.add_argument(
		"--base-url",
		default="http://localhost:8080",
		help="Server base URL (default: %(default)s)",
	)
	parser.add_argument("--username", help="Username or email")
	args = parser.parse_args()

	username = args.username or input("Username or email: ").strip()
	password = getpass.getpass("Password: ")
	if not username or not password:
		print("Username and password are required", file=sys.stderr)
		return 1

	with TsmClient(args.base_url) as client:
		try:
			auth = client.login(username, password)
			me = client.me()
		except httpx.HTTPStatusError as exc:
			print(f"HTTP {exc.response.status_code}: {exc.response.text}", file=sys.stderr)
			return 1
		except httpx.HTTPError as exc:
			print(f"Request failed: {exc}", file=sys.stderr)
			return 1

		print("Authenticated as:")
		print(json.dumps(auth.get("user") or me, indent=2))
		print("\n/api/auth/me:")
		print(json.dumps(me, indent=2))
		print(f"\naccessToken: {client.access_token[:24]}…")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
