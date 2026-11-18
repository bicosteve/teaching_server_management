"""Minimal http client for Teaching Server Management API."""

from __future__ import annotations

from typing import Any

import httpx

from config import API_TIMEOUT_SECONDS, APPROVED_OPERATION_STATUS


class TsmClient:
	"""httpx client that logs in, stores JWTs, and calls the TSM API with Bearer auth."""

	def __init__(self, base_url: str, timeout: float = API_TIMEOUT_SECONDS) -> None:
		self.base_url = base_url.rstrip("/")
		self.http = httpx.Client(base_url=self.base_url, timeout=timeout)
		self.access_token: str | None = None
		self.refresh_token: str | None = None
		self.user: dict[str, Any] | None = None

	def close(self) -> None:
		self.http.close()

	def __enter__(self) -> TsmClient:
		return self

	def __exit__(self, *args: object) -> None:
		self.close()

	def login(self, username_or_email: str, password: str) -> dict[str, Any]:
		response = self.http.post(
			"/api/auth/login",
			json={"usernameOrEmail": username_or_email, "password": password},
		)
		response.raise_for_status()
		payload = response.json()
		self.store_tokens(payload)
		return payload

	def refresh(self) -> dict[str, Any]:
		if not self.refresh_token:
			raise RuntimeError("No refresh token; call login() first")
		response = self.http.post(
			"/api/auth/refresh",
			json={"refreshToken": self.refresh_token},
		)
		response.raise_for_status()
		payload = response.json()
		self.store_tokens(payload)
		return payload

	def me(self) -> dict[str, Any]:
		return self.get("/api/auth/me")

	def list_operations(self, status: str | None = None) -> list[dict[str, Any]]:
		params = {"status": status} if status else None
		result = self.get("/api/admin/operations", params=params)
		if result is None:
			return []
		if not isinstance(result, list):
			raise TypeError("Expected a list of server operations")
		return result

	def list_approved_operations(self) -> list[dict[str, Any]]:
		return self.list_operations(status=APPROVED_OPERATION_STATUS)

	def get(self, path: str, **kwargs: Any) -> Any:
		return self.request("GET", path, **kwargs)

	def post(self, path: str, **kwargs: Any) -> Any:
		return self.request("POST", path, **kwargs)

	def request(self, method: str, path: str, **kwargs: Any) -> Any:
		headers = dict(kwargs.pop("headers", {}) or {})
		if self.access_token:
			headers["Authorization"] = f"Bearer {self.access_token}"
		response = self.http.request(method, path, headers=headers, **kwargs)
		response.raise_for_status()
		if response.status_code == 204 or not response.content:
			return None
		return response.json()

	def store_tokens(self, payload: dict[str, Any]) -> None:
		self.access_token = payload["accessToken"]
		self.refresh_token = payload["refreshToken"]
		self.user = payload.get("user")
