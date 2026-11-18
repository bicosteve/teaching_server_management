"""Rich screen frames for the approved-operations console."""

from __future__ import annotations

from typing import Any

from rich.align import Align
from rich.console import Group, RenderableType
from rich.layout import Layout
from rich.panel import Panel
from rich.table import Table
from rich.text import Text

from ui.state import UiState, op_id

LIST_HOTKEYS = "Space Check   a All/None   Up/Down Move   Enter Detail   x Run   r Refresh   q Quit"
DETAIL_HOTKEYS = "Esc Back   q Quit"
DASH = "-"


def build_screen(*, base_url: str, user: dict[str, Any] | None, state: UiState, height: int) -> Layout:
	layout = Layout()
	layout.split_column(
		Layout(name="header", size=3),
		Layout(name="body", ratio=1),
		Layout(name="status", size=1),
		Layout(name="footer", size=1),
	)

	checked_n = len(state.checked)
	header = Text.assemble(
		("TSM", "bold cyan"),
		("  approved", "bold"),
		(f" ({len(state.operations)})", "dim"),
		(f"  |  checked {checked_n}", "yellow" if checked_n else "dim"),
		("  |  ", "dim"),
		(base_url, "cyan"),
		("  |  ", "dim"),
		(user_label(user), "green"),
	)
	layout["header"].update(Panel(header, border_style="cyan", padding=(0, 1)))

	if state.detail is None:
		layout["body"].update(operations_body(state, max(1, height - 5)))
		layout["footer"].update(Text(f" {LIST_HOTKEYS}", style="bold white on dark_blue"))
	else:
		layout["body"].update(detail_body(state.detail))
		layout["footer"].update(Text(f" {DETAIL_HOTKEYS}", style="bold white on dark_blue"))

	layout["status"].update(status_line(state.status))
	return layout


def operations_body(state: UiState, body_height: int) -> RenderableType:
	if not state.operations:
		return Align.center(
			Panel("[yellow]No APPROVED operations waiting.[/yellow]", border_style="yellow", width=48),
			vertical="middle",
		)

	table = Table(expand=True, show_edge=False, pad_edge=False, box=None)
	table.add_column("", width=3, justify="center")
	table.add_column("#", style="dim", width=4, justify="right")
	table.add_column("Type", style="cyan", no_wrap=True)
	table.add_column("Target", overflow="ellipsis", no_wrap=True)
	table.add_column("Requested by", overflow="ellipsis", no_wrap=True)
	table.add_column("Approved by", overflow="ellipsis", no_wrap=True)
	table.add_column("Approved at", style="dim", no_wrap=True)
	table.add_column("Id", style="dim", overflow="ellipsis", no_wrap=True)

	visible = max(1, body_height - 3)
	start = max(0, state.cursor - visible + 1) if state.cursor >= visible else 0

	for index, op in enumerate(state.operations):
		if index < start or index >= start + visible:
			continue
		oid = op_id(op, index)
		table.add_row(
			Text("[x]" if oid in state.checked else "[ ]"),
			str(index + 1),
			display(op.get("type")),
			display(op.get("target")),
			display(op.get("requestedByName") or op.get("requestedBy")),
			display(op.get("approvedByName") or op.get("approvedBy")),
			format_instant(op.get("approvedAt")),
			short_id(op.get("id")),
			style="reverse bold" if index == state.cursor else "",
		)

	return Panel(table, title="Approved", border_style="blue", padding=(0, 1))


def detail_body(operation: dict[str, Any]) -> RenderableType:
	meta = Table(show_header=False, box=None, padding=(0, 1), expand=True)
	meta.add_column("Field", style="bold dim", width=14)
	meta.add_column("Value", overflow="fold")
	for label, value in (
		("Id", operation.get("id")),
		("Type", operation.get("type")),
		("Status", operation.get("status")),
		("Target", operation.get("target")),
		("Requested by", operation.get("requestedByName") or operation.get("requestedBy")),
		("Approved by", operation.get("approvedByName") or operation.get("approvedBy")),
		("Created at", format_instant(operation.get("createdAt"))),
		("Approved at", format_instant(operation.get("approvedAt"))),
	):
		meta.add_row(label, display(value))

	payload = operation.get("payload") if isinstance(operation.get("payload"), dict) else {}
	payload_table = Table(show_header=True, expand=True, box=None, pad_edge=False)
	payload_table.add_column("Key", style="cyan", width=20)
	payload_table.add_column("Value", overflow="fold")
	if payload:
		for key, value in payload.items():
			payload_table.add_row(str(key), "null" if value is None else str(value))
	else:
		payload_table.add_row(DASH, "[dim]empty[/dim]")

	return Panel(
		Group(
			meta,
			Text(""),
			Text("Payload", style="bold"),
			payload_table,
			Text(""),
			Text("Check rows with Space, then x to run (sudo required).", style="dim italic"),
		),
		title=f"Detail | {operation.get('type') or 'unknown'}",
		border_style="magenta",
		padding=(0, 1),
	)


def status_line(status: str | None) -> Text:
	if not status:
		return Text("")
	lower = status.lower()
	if lower.startswith("error"):
		style = "red"
	elif lower.startswith(("ok", "ran")):
		style = "green"
	else:
		style = "yellow"
	return Text(f" {status}", style=style)


def user_label(user: dict[str, Any] | None) -> str:
	if not user:
		return "unknown"
	username, role = user.get("username"), user.get("role")
	if username and role:
		return f"{username} ({role})"
	return str(username or user.get("email") or user.get("id") or "unknown")


def format_instant(value: Any) -> str:
	if value is None:
		return DASH
	return str(value).replace("T", " ").removesuffix("Z").split(".")[0]


def short_id(value: Any) -> str:
	text = str(value or DASH)
	return text if len(text) <= 8 else f"{text[:8]}..."


def display(value: Any) -> str:
	return DASH if value is None else str(value)
