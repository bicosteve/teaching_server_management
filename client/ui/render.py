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
DETAIL_HOTKEYS = "Up/Down Scroll   Esc Back   q Quit"
DASH = "-"


def build_screen(
	*,
	base_url: str,
	user: dict[str, Any] | None,
	state: UiState,
	height: int,
	width: int,
) -> Layout:
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

	body_height = max(1, height - 5)
	if state.detail is None:
		layout["body"].update(operations_body(state, body_height))
		layout["footer"].update(Text(f" {LIST_HOTKEYS}", style="bold white on dark_blue"))
	else:
		layout["body"].update(detail_body(state, body_height, width))
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


def detail_body(state: UiState, body_height: int, width: int) -> RenderableType:
	operation = state.detail
	assert operation is not None

	inner_width = max(16, width - 4)
	lines = detail_lines(operation, inner_width)
	visible = max(1, body_height - 2)
	max_scroll = max(0, len(lines) - visible)
	state.detail_scroll = min(state.detail_scroll, max_scroll)
	window = lines[state.detail_scroll : state.detail_scroll + visible]

	title = f"Detail | {operation.get('type') or 'unknown'}"
	if max_scroll:
		lo = state.detail_scroll + 1
		hi = state.detail_scroll + len(window)
		title = f"{title}  ({lo}-{hi}/{len(lines)})"

	return Panel(
		Group(*window) if window else Text(""),
		title=title,
		border_style="magenta",
		padding=(0, 1),
	)


def detail_lines(operation: dict[str, Any], width: int) -> list[Text]:
	"""One Text per terminal row (newlines in values are expanded)."""
	lines: list[Text] = []
	label_w = 14

	def add_labeled(label: str, value: str, *, style: str = "bold dim", key_w: int = label_w) -> None:
		rows = soft_wrap(value, max(8, width - key_w - 2))
		lines.append(Text.assemble((f"{label:<{key_w}}", style), ("  ", ""), (rows[0], "")))
		pad = f"{'':<{key_w}}  "
		lines.extend(Text(pad + row) for row in rows[1:])

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
		add_labeled(label, display(value))

	lines += [Text(""), Text("Payload", style="bold")]

	payload = operation.get("payload") if isinstance(operation.get("payload"), dict) else {}
	if not payload:
		lines.append(Text(f"{DASH}  empty", style="dim"))
	else:
		for key, value in payload.items():
			text = "null" if value is None else str(value)
			if key == "description" or "\n" in text:
				lines.append(Text(str(key), style="cyan bold"))
				lines.extend(Text(row) for row in soft_wrap(text, width))
			else:
				add_labeled(str(key), text, style="cyan", key_w=min(20, max(8, width // 3)))

	lines.append(Text(""))
	lines.extend(
		Text(row, style="dim italic")
		for row in soft_wrap("Check rows with Space, then x to run (sudo required).", width)
	)
	return lines


def soft_wrap(text: str, width: int) -> list[str]:
	"""Wrap to width; each input newline becomes its own row."""
	width = max(1, width)
	rows: list[str] = []
	for line in text.splitlines() or [""]:
		if not line:
			rows.append("")
		else:
			rows.extend(line[i : i + width] for i in range(0, len(line), width))
	return rows or [""]


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
