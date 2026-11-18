"""Fullscreen Rich console for approved server operations."""

from __future__ import annotations

from typing import Any

import httpx
from rich.console import Console

from api.client import TsmClient
from operations.dispatcher import OperationDispatcher
from ui.keys import read_key
from ui.prompts import ask_login, ask_password
from ui.render import build_screen
from ui.state import UiState


def run_ui(base_url: str, username: str | None = None) -> int:
	return App(base_url, username).run()


class App:
	def __init__(self, base_url: str, username: str | None = None) -> None:
		self.base_url = base_url
		self.username = username
		self.console = Console()
		self.state = UiState()
		self.user: dict[str, Any] | None = None

	def run(self) -> int:
		try:
			username, password = ask_login(self.username)
		except KeyboardInterrupt:
			self.console.print("\n[dim]Cancelled.[/dim]")
			return 130
		if not username or not password:
			self.console.print("[red]Username and password are required.[/red]")
			return 1

		with TsmClient(self.base_url) as client:
			try:
				client.login(username, password)
				self.user = client.me() or client.user
			except httpx.HTTPError as exc:
				self.console.print(f"[red]Login failed:[/red] {http_error(exc)}")
				return 1

			role = (self.user or {}).get("role")
			if role and role != "ADMIN":
				self.state.status = (
					f"Warning: signed in as {role}; /api/admin/operations requires ADMIN."
				)
			self.reload(client, keep_status=True)

			while True:
				if self.browse(client) == "quit":
					return 0
				self.run_checked()

	def browse(self, client: TsmClient) -> str:
		with self.console.screen() as screen:
			self.redraw(screen, client)
			while True:
				key = read_key()
				if key in ("q", "ctrl-c"):
					return "quit"

				if self.state.detail is not None:
					if key in ("esc", "backspace"):
						self.state.close_detail()
					self.redraw(screen, client)
					continue

				if key in ("r", "R"):
					self.reload(client)
				elif key in ("up", "k"):
					self.state.move(-1)
				elif key in ("down", "j"):
					self.state.move(1)
				elif key == " ":
					self.state.toggle_check()
				elif key in ("a", "A"):
					self.state.toggle_all()
				elif key in ("enter", "v", "l", "right"):
					self.state.open_detail()
				elif key in ("x", "X"):
					return "run"
				else:
					continue
				self.redraw(screen, client)

	def redraw(self, screen: Any, client: TsmClient) -> None:
		screen.update(
			build_screen(
				base_url=client.base_url,
				user=self.user,
				state=self.state,
				height=self.console.size.height,
			)
		)

	def reload(self, client: TsmClient, *, keep_status: bool = False) -> None:
		preserve = self.state.status if keep_status else None
		try:
			self.state.operations = client.list_approved_operations()
			self.state.status = preserve or (
				f"Loaded {len(self.state.operations)} approved operation(s)."
			)
		except httpx.HTTPError as exc:
			self.state.operations = []
			self.state.status = f"Error: {http_error(exc)}"
		self.state.prune_checked()
		self.state.clamp_cursor()
		self.state.detail = None

	def run_checked(self) -> None:
		selected = self.state.checked_operations()
		if not selected:
			self.state.status = "Error: nothing checked - Space to check, then x to run."
			return

		self.console.print()
		self.console.print(
			f"[bold]Run {len(selected)} checked operation(s).[/bold] "
			"Sudo is required for group changes."
		)
		sudo_password = ask_password("Sudo password:")
		if sudo_password is None:
			self.state.status = "Run cancelled."
			return

		dispatcher = OperationDispatcher(sudo_password=sudo_password)
		ok = 0
		failures: list[str] = []
		for op in selected:
			label = f"{op.get('type') or '?'}/{op.get('target') or op.get('id') or '?'}"
			try:
				dispatcher.execute(op)
				ok += 1
				if op.get("id") is not None:
					self.state.checked.discard(str(op["id"]))
			except Exception as exc:  # noqa: BLE001 - show per-op failure in the status bar
				failures.append(f"{label}: {exc}")

		self.state.detail = None
		if failures:
			extra = f" (+{len(failures) - 1} more)" if len(failures) > 1 else ""
			self.state.status = f"Error: ran {ok}/{len(selected)} - {failures[0]}{extra}"
		else:
			self.state.status = f"Ran {ok} operation(s) successfully."


def http_error(exc: httpx.HTTPError) -> str:
	if isinstance(exc, httpx.HTTPStatusError):
		body = (exc.response.text or "").strip()
		msg = f"HTTP {exc.response.status_code}"
		return f"{msg} - {body[:120]}" if body else msg
	return str(exc)
