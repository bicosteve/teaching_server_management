"""Questionary prompts used by the console."""

from __future__ import annotations

import questionary


def ask_text(message: str) -> str | None:
	return questionary.text(
		message,
		qmark="",
		validate=lambda value: bool(value.strip()) or "Required",
	).ask()


def ask_password(message: str) -> str | None:
	return questionary.password(
		message,
		qmark="",
		validate=lambda value: bool(value) or "Required",
	).ask()


def ask_login(username: str | None = None) -> tuple[str, str]:
	answered = username or ask_text("Username or email:")
	if not answered:
		return "", ""
	return answered.strip(), ask_password("Password:") or ""
