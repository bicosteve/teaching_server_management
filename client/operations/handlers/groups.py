"""Linux group helpers for course operation handlers."""

from __future__ import annotations

import re
import subprocess

from config import (
	COURSE_GROUP_DIGIT_PREFIX,
	GETENT,
	GROUP_COMMAND_TIMEOUT,
	GROUP_LOOKUP_TIMEOUT,
	LINUX_GROUP_NAME_MAX_LEN,
	LINUX_GROUP_NAME_PATTERN,
	GROUPADD,
	GROUPDEL,
	GROUPMOD,
	SUDO,
)


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


def validate_group_name(group_name: str) -> None:
	if not isinstance(group_name, str) or not group_name:
		raise ValueError("Missing or invalid group name")
	if not LINUX_GROUP_NAME_PATTERN.fullmatch(group_name):
		raise ValueError(f"Invalid Linux group name: {group_name}")


def course_group_name(name: str, academic_year: str) -> str:
	"""Derive a Linux group name from course payload fields."""
	base = re.sub(r"[^a-z0-9]+", "-", name.strip().lower()).strip("-")
	year = re.sub(r"[^a-z0-9]+", "-", academic_year.strip().lower()).strip("-")
	if not base or not year:
		raise ValueError("Cannot derive group name from empty name or academic_year")

	suffix = f"-{year}"
	budget = LINUX_GROUP_NAME_MAX_LEN - len(suffix) - len(COURSE_GROUP_DIGIT_PREFIX)
	if budget < 1:
		raise ValueError(f"Academic year slug too long for a Linux group name: {year}")

	base = base[:budget].rstrip("-")
	if not base:
		raise ValueError("Cannot derive group name from empty name or academic_year")

	candidate = f"{COURSE_GROUP_DIGIT_PREFIX}{base}{suffix}"
	validate_group_name(candidate)
	return candidate


def run_sudo(command: list[str], password: str) -> None:
	"""Run command under sudo, reading password from stdin (not stored)."""
	reported = list(command)
	completed = subprocess.run(
		[SUDO, "-S", "-k", "--", *command],
		input=f"{password}\n",
		capture_output=True,
		text=True,
		timeout=GROUP_COMMAND_TIMEOUT,
		check=False,
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


class LinuxGroupManager:
	"""Create, rename, and delete local Linux groups."""

	def create(self, group_name: str, *, sudo_password: str) -> dict:
		validate_group_name(group_name)
		if self.exists(group_name):
			raise RuntimeError(f"Group already exists: {group_name}")
		run_sudo([GROUPADD, group_name], sudo_password)
		return self.result("CREATE", group_name, f"Group {group_name} created successfully")

	def delete(self, group_name: str, *, sudo_password: str) -> dict:
		validate_group_name(group_name)
		if not self.exists(group_name):
			raise RuntimeError(f"Group does not exist: {group_name}")
		run_sudo([GROUPDEL, group_name], sudo_password)
		return self.result("DELETE", group_name, f"Group {group_name} deleted successfully")

	def rename(self, old_name: str, new_name: str, *, sudo_password: str) -> dict:
		validate_group_name(old_name)
		validate_group_name(new_name)
		if old_name == new_name:
			return {
				**self.result("RENAME", new_name, f"Group {old_name} already has the requested name"),
				"previous_group_name": old_name,
			}
		if not self.exists(old_name):
			raise RuntimeError(f"Group does not exist: {old_name}")
		if self.exists(new_name):
			raise RuntimeError(f"Group already exists: {new_name}")
		run_sudo([GROUPMOD, "-n", new_name, old_name], sudo_password)
		return {
			**self.result("RENAME", new_name, f"Group {old_name} renamed to {new_name}"),
			"previous_group_name": old_name,
		}

	def exists(self, group_name: str) -> bool:
		result = subprocess.run(
			[GETENT, "group", group_name],
			capture_output=True,
			text=True,
			timeout=GROUP_LOOKUP_TIMEOUT,
			check=False,
		)
		return result.returncode == 0

	@staticmethod
	def result(action: str, group_name: str, message: str) -> dict:
		return {"action": action, "group_name": group_name, "message": message}
