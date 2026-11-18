"""Mutable UI state for the approved-operations console."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class UiState:
	operations: list[dict[str, Any]] = field(default_factory=list)
	cursor: int = 0
	checked: set[str] = field(default_factory=set)
	detail: dict[str, Any] | None = None
	status: str | None = None

	def clamp_cursor(self) -> None:
		self.cursor = min(self.cursor, max(0, len(self.operations) - 1))

	def prune_checked(self) -> None:
		self.checked &= self.all_ids()

	def move(self, delta: int) -> None:
		if not self.operations:
			return
		self.cursor = (self.cursor + delta) % len(self.operations)
		self.status = None

	def open_detail(self) -> None:
		if not self.operations:
			self.status = "No approved operations to open."
			return
		self.detail = self.operations[self.cursor]
		self.status = None

	def close_detail(self) -> None:
		self.detail = None
		self.status = None

	def toggle_check(self) -> None:
		if not self.operations:
			self.status = "No approved operations to check."
			return
		oid = op_id(self.operations[self.cursor], self.cursor)
		self.checked.symmetric_difference_update({oid})
		label = "Checked" if oid in self.checked else "Unchecked"
		self.status = f"{label} | {len(self.checked)} selected."

	def toggle_all(self) -> None:
		if not self.operations:
			self.status = "No approved operations to select."
			return
		ids = self.all_ids()
		if self.checked >= ids:
			self.checked.clear()
			self.status = "Unchecked all."
		else:
			self.checked |= ids
			self.status = f"Checked all ({len(self.checked)})."

	def checked_operations(self) -> list[dict[str, Any]]:
		return [op for i, op in enumerate(self.operations) if op_id(op, i) in self.checked]

	def all_ids(self) -> set[str]:
		return {op_id(op, i) for i, op in enumerate(self.operations)}


def op_id(operation: dict[str, Any], index: int) -> str:
	raw = operation.get("id")
	return str(raw) if raw is not None else f"index:{index}"
