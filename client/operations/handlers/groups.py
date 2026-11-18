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


class LinuxGroupManager:
	"""Create, rename, and delete local Linux groups."""

	def __init__(self, sudo_password: str | None = None) -> None:
		self.sudo_password = sudo_password

	def create(self, group_name: str) -> dict:
		validate_group_name(group_name)
		if self.exists(group_name):
			raise RuntimeError(f"Group already exists: {group_name}")
		self.run([GROUPADD, group_name])
		return self.result("CREATE", group_name, f"Group {group_name} created successfully")

	def delete(self, group_name: str) -> dict:
		validate_group_name(group_name)
		if not self.exists(group_name):
			raise RuntimeError(f"Group does not exist: {group_name}")
		self.run([GROUPDEL, group_name])
		return self.result("DELETE", group_name, f"Group {group_name} deleted successfully")

	def rename(self, old_name: str, new_name: str) -> dict:
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
		self.run([GROUPMOD, "-n", new_name, old_name])
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

	def run(self, command: list[str]) -> None:
		reported = list(command)
		if self.sudo_password is None:
			argv, stdin = command, None
		else:
			argv, stdin = [SUDO, "-S", "--", *command], f"{self.sudo_password}\n"

		completed = subprocess.run(
			argv,
			input=stdin,
			capture_output=True,
			text=True,
			timeout=GROUP_COMMAND_TIMEOUT,
			check=False,
		)
		if completed.returncode != 0:
			raise ShellCommandError(
				reported,
				completed.returncode,
				stdout=completed.stdout or "",
				stderr=completed.stderr or "",
			)

	@staticmethod
	def result(action: str, group_name: str, message: str) -> dict:
		return {"action": action, "group_name": group_name, "message": message}
