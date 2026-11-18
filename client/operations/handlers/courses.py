"""Handlers for CREATE/UPDATE/DELETE_COURSE server operations."""

from __future__ import annotations

from typing import Any

from operations.base import OperationHandler
from operations.handlers.groups import LinuxGroupManager, course_group_name

UPDATE_FIELDS = {
	"name",
	"description",
	"academic_year",
	"previous_name",
	"previous_academic_year",
}


class CourseHandler(OperationHandler):
	def __init__(self, groups: LinuxGroupManager | None = None) -> None:
		self.groups = groups or LinuxGroupManager()

	def require_group_name(self, params: dict[str, Any]) -> str:
		return course_group_name(require_str(params, "name"), require_str(params, "academic_year"))

	def annotate(self, result: dict, operation: str, params: dict[str, Any]) -> dict:
		return {
			**result,
			"operation": operation,
			"course_name": params["name"],
			"academic_year": params["academic_year"],
		}


class CreateCourseHandler(CourseHandler):
	def validate(self, params: dict[str, Any]) -> None:
		self.require_group_name(params)
		optional_str(params, "description")

	def execute(self, params: dict[str, Any]) -> dict:
		return self.annotate(self.groups.create(self.require_group_name(params)), "CREATE_COURSE", params)


class DeleteCourseHandler(CourseHandler):
	def validate(self, params: dict[str, Any]) -> None:
		self.require_group_name(params)

	def execute(self, params: dict[str, Any]) -> dict:
		return self.annotate(self.groups.delete(self.require_group_name(params)), "DELETE_COURSE", params)


class UpdateCourseHandler(CourseHandler):
	"""Rename the Linux group when name/year change; no-op for description-only.
	"""

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

	def execute(self, params: dict[str, Any]) -> dict:
		if "name" not in params and "academic_year" not in params:
			return {
				"action": "NOOP",
				"operation": "UPDATE_COURSE",
				"message": "No Linux group changes required",
			}

		old_name = params["previous_name"]
		old_year = params["previous_academic_year"]
		new_name = params.get("name") or old_name
		new_year = params.get("academic_year") or old_year
		result = self.groups.rename(
			course_group_name(old_name, old_year),
			course_group_name(new_name, new_year),
		)
		return {
			**result,
			"operation": "UPDATE_COURSE",
			"course_name": new_name,
			"academic_year": new_year,
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
