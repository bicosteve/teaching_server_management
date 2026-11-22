"""Local shell helpers for privileged teaching-server operations."""

from __future__ import annotations

import subprocess

from config import COMMAND_TIMEOUT, SUDO


class ShellCommandError(RuntimeError):
	"""Raised when a local shell command fails."""

	def __init__(
		self,
		command: list[str],
		returncode: int,
		stdout: str = "",
		stderr: str = "",
	) -> None:
		detail = (stderr or stdout or "").strip() or f"exit {returncode}"
		super().__init__(f"{' '.join(command)} failed: {detail}")


class SudoAuthenticationError(RuntimeError):
	"""Raised when sudo rejects the password. Local-only, do not report to the server."""


def run_command(
	command: list[str],
	*,
	timeout: int = COMMAND_TIMEOUT,
	input_text: str | None = None,
) -> subprocess.CompletedProcess[str]:
	"""Run a local command and return the completed process (no exception on non-zero)."""
	return subprocess.run(
		command,
		input=input_text,
		capture_output=True,
		text=True,
		timeout=timeout,
		check=False,
	)


def run_sudo(command: list[str], password: str) -> None:
	"""Run command under sudo, reading password from stdin (not stored)."""
	reported = list(command)
	completed = run_command(
		[SUDO, "-S", "-k", "--", *command],
		input_text=f"{password}\n",
	)
	if completed.returncode == 0:
		return

	stderr = completed.stderr or ""
	stdout = completed.stdout or ""
	if is_sudo_auth_failure(stderr, stdout):
		raise SudoAuthenticationError((stderr or stdout).strip() or "incorrect sudo password")
	raise ShellCommandError(
		reported,
		completed.returncode,
		stdout=stdout,
		stderr=stderr,
	)


def is_sudo_auth_failure(stderr: str, stdout: str = "") -> bool:
	text = f"{stderr}\n{stdout}".lower()
	return any(
		hint in text
		for hint in (
			"incorrect password",
			"sorry, try again",
			"a password is required",
			"authentication failure",
			"authentication failed",
		)
	)
