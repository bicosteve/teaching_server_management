"""Linux group helpers for course operation handlers."""

from __future__ import annotations

import re

from config import (
	COURSE_GROUP_DIGIT_PREFIX,
	GETENT,
	GROUP_LOOKUP_TIMEOUT,
	LINUX_GROUP_NAME_MAX_LEN,
	LINUX_GROUP_NAME_PATTERN,
	GROUPADD,
	GROUPDEL,
	GROUPMOD,
)
from operations.shell import run_command, run_sudo


def validate_group_name(group_name: str) -> None:
	if not isinstance(group_name, str) or not group_name:
		raise ValueError("Missing or invalid group name")
	if not LINUX_GROUP_NAME_PATTERN.fullmatch(group_name):
		raise ValueError(f"Invalid Linux group name: {group_name}")


def slugify(value: str) -> str:
	"""Normalize a label to a lowercase hyphenated slug."""
	if not isinstance(value, str):
		raise ValueError("Missing or invalid value for slug")
	return re.sub(r"[^a-z0-9]+", "-", value.strip().lower()).strip("-")


def course_group_name(name: str, academic_year: str) -> str:
	"""Derive a Linux group name from course payload fields."""
	base = slugify(name)
	year = slugify(academic_year)
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
		result = run_command([GETENT, "group", group_name], timeout=GROUP_LOOKUP_TIMEOUT)
		return result.returncode == 0

	@staticmethod
	def result(action: str, group_name: str, message: str) -> dict:
		return {"action": action, "group_name": group_name, "message": message}
