"""Fullscreen Rich console for approved server operations."""

from __future__ import annotations

import json
from typing import Any

import httpx
from rich.console import Console

from api.client import TsmClient
from operations.dispatcher import OperationDispatcher
from operations.handlers.groups import SudoAuthenticationError
from ui.keys import mouse_tracking, read_key
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
				self.run_checked(client)

	def browse(self, client: TsmClient) -> str:
		with self.console.screen() as screen, mouse_tracking():
			self.redraw(screen, client)
			while True:
				key = read_key()
				if key == "ignore":
					continue
				if key in ("q", "ctrl-c"):
					return "quit"

				if self.state.detail is not None:
					if key in ("esc", "backspace", "left"):
						self.state.close_detail()
					elif key in ("up", "k"):
						self.state.scroll_detail(-1)
					elif key in ("down", "j"):
						self.state.scroll_detail(1)
					else:
						continue
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
		size = self.console.size
		screen.update(
			build_screen(
				base_url=client.base_url,
				user=self.user,
				state=self.state,
				height=size.height,
				width=size.width,
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
		self.state.detail_scroll = 0

	def run_checked(self, client: TsmClient) -> None:
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

		dispatcher = OperationDispatcher()
		ok = 0
		failures: list[str] = []
		for op in selected:
			label = f"{op.get('type') or '?'}/{op.get('target') or op.get('id') or '?'}"
			op_id = op.get("id")
			try:
				result = dispatcher.execute(op, sudo_password=sudo_password)
			except SudoAuthenticationError as exc:
				# Wrong sudo password is local-only; do not report failure to the server.
				self.state.status = f"Error: sudo authentication failed - {exc}"
				return
			except Exception as exc:  
				detail = str(exc)
				feedback_error = self.try_report_feedback(
					client, op_id, success=False, execution_result=detail
				)
				if feedback_error:
					detail = f"{detail}; feedback error: {feedback_error}"
				failures.append(f"{label}: {detail}")
				continue

			feedback_error = self.try_report_feedback(
				client, op_id, success=True, execution_result=success_result(result)
			)
			if feedback_error:
				failures.append(f"{label}: local ok, feedback failed: {feedback_error}")
				continue

			ok += 1
			if op_id is not None:
				self.state.checked.discard(str(op_id))

		summary = (
			f"Error: ran {ok}/{len(selected)} - {failures[0]}"
			+ (f" (+{len(failures) - 1} more)" if len(failures) > 1 else "")
			if failures
			else f"Ran {ok} operation(s) successfully."
		)
		self.reload(client, keep_status=True)
		self.state.status = summary

	def try_report_feedback(
		self,
		client: TsmClient,
		operation_id: Any,
		*,
		success: bool,
		execution_result: str,
	) -> str | None:
		if operation_id is None:
			return "missing operation id"
		try:
			client.report_execution_feedback(
				operation_id,
				success=success,
				execution_result=execution_result,
			)
		except Exception as exc:  # noqa: BLE001
			return str(exc)
		return None


def success_result(result: dict[str, Any]) -> str:
	message = result.get("message")
	if isinstance(message, str) and message.strip():
		return message.strip()
	return json.dumps(result, ensure_ascii=False, sort_keys=True)


def http_error(exc: httpx.HTTPError) -> str:
	if isinstance(exc, httpx.HTTPStatusError):
		body = (exc.response.text or "").strip()
		msg = f"HTTP {exc.response.status_code}"
		return f"{msg} - {body[:120]}" if body else msg
	return str(exc)
