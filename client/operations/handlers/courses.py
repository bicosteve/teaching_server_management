"""Handlers for CREATE/UPDATE/DELETE_COURSE server operations."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from operations.base import OperationHandler
from operations.handlers.files import CourseDirectoryManager, course_directory_path
from operations.handlers.groups import LinuxGroupManager, course_group_name

UPDATE_FIELDS = {
	"name",
	"description",
	"academic_year",
	"previous_name",
	"previous_academic_year",
	"previous_description",
}


class CourseHandler(OperationHandler):
	def __init__(
		self,
		groups: LinuxGroupManager | None = None,
		files: CourseDirectoryManager | None = None,
	) -> None:
		self.groups = groups or LinuxGroupManager()
		self.files = files or CourseDirectoryManager()

	def require_group_name(self, params: dict[str, Any]) -> str:
		return course_group_name(require_str(params, "name"), require_str(params, "academic_year"))

	def require_directory_path(self, params: dict[str, Any]) -> Path:
		return course_directory_path(require_str(params, "name"), require_str(params, "academic_year"))


class CreateCourseHandler(CourseHandler):
	def validate(self, params: dict[str, Any]) -> None:
		self.require_group_name(params)
		self.require_directory_path(params)
		optional_str(params, "description")

	def execute(self, params: dict[str, Any], *, sudo_password: str) -> dict:
		group = self.require_group_name(params)
		path = self.require_directory_path(params)
		self.groups.create(group, sudo_password=sudo_password)
		self.files.create(path, group_name=group, sudo_password=sudo_password)
		return {
			"action": "CREATE",
			"operation": "CREATE_COURSE",
			"group_name": group,
			"path": str(path),
			"course_name": params["name"],
			"academic_year": params["academic_year"],
			"message": f"Group {group} and directory {path} created successfully",
		}


class DeleteCourseHandler(CourseHandler):
	def validate(self, params: dict[str, Any]) -> None:
		self.require_group_name(params)
		self.require_directory_path(params)

	def execute(self, params: dict[str, Any], *, sudo_password: str) -> dict:
		group = self.require_group_name(params)
		path = self.require_directory_path(params)
		self.files.delete(path, sudo_password=sudo_password)
		self.groups.delete(group, sudo_password=sudo_password)
		return {
			"action": "DELETE",
			"operation": "DELETE_COURSE",
			"group_name": group,
			"path": str(path),
			"course_name": params["name"],
			"academic_year": params["academic_year"],
			"message": f"Directory {path} and group {group} deleted successfully",
		}


class UpdateCourseHandler(CourseHandler):
	def validate(self, params: dict[str, Any]) -> None:
		if not params:
			raise ValueError("UPDATE_COURSE payload must not be empty")
		unknown = set(params) - UPDATE_FIELDS
		if unknown:
			raise ValueError(f"Unsupported UPDATE_COURSE fields: {sorted(unknown)}")
		for key in UPDATE_FIELDS:
			optional_str(params, key)

		if "name" in params or "academic_year" in params:
			old_name = require_str(params, "previous_name")
			old_year = require_str(params, "previous_academic_year")
			new_name = params.get("name") or old_name
			new_year = params.get("academic_year") or old_year
			course_group_name(old_name, old_year)
			course_group_name(require_value(new_name, "name"), require_value(new_year, "academic_year"))

	def execute(self, params: dict[str, Any], *, sudo_password: str) -> dict:
		if "name" not in params and "academic_year" not in params:
			return {
				"action": "NOOP",
				"operation": "UPDATE_COURSE",
				"message": "No Linux group or directory changes required",
			}

		old_name = params["previous_name"]
		old_year = params["previous_academic_year"]
		new_name = params.get("name") or old_name
		new_year = params.get("academic_year") or old_year
		old_group, new_group = course_group_name(old_name, old_year), course_group_name(new_name, new_year)
		old_path, new_path = course_directory_path(old_name, old_year), course_directory_path(new_name, new_year)

		self.groups.rename(old_group, new_group, sudo_password=sudo_password)
		self.files.rename(old_path, new_path, group_name=new_group, sudo_password=sudo_password)
		return {
			"action": "RENAME",
			"operation": "UPDATE_COURSE",
			"group_name": new_group,
			"previous_group_name": old_group,
			"path": str(new_path),
			"previous_path": str(old_path),
			"course_name": new_name,
			"academic_year": new_year,
			"message": f"Group {old_group} → {new_group}; directory {old_path} → {new_path}",
		}


def require_str(params: dict[str, Any], key: str) -> str:
	return require_value(params.get(key), key)


def require_value(value: Any, key: str) -> str:
	if not isinstance(value, str) or not value.strip():
		raise ValueError(f"Missing or invalid {key}")
	return value


def optional_str(params: dict[str, Any], key: str) -> None:
	if key in params and params[key] is not None and not isinstance(params[key], str):
		raise ValueError(f"Invalid {key}")
