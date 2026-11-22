from __future__ import annotations

from typing import Any

from operations.base import OperationHandler
from operations.handlers.courses import (
	CreateCourseHandler,
	DeleteCourseHandler,
	UpdateCourseHandler,
)
from operations.handlers.files import CourseDirectoryManager
from operations.handlers.groups import LinuxGroupManager


class OperationDispatcher:
	"""Route a server operation dict to the matching handler."""

	def __init__(self, handlers: dict[str, OperationHandler] | None = None) -> None:
		if handlers is not None:
			self.handlers = handlers
			return
		groups = LinuxGroupManager()
		files = CourseDirectoryManager()
		self.handlers = {
			"CREATE_COURSE": CreateCourseHandler(groups, files),
			"UPDATE_COURSE": UpdateCourseHandler(groups, files),
			"DELETE_COURSE": DeleteCourseHandler(groups, files),
		}

	def execute(self, operation: dict[str, Any], *, sudo_password: str) -> dict:
		op_type = operation.get("type")
		if not isinstance(op_type, str) or not op_type:
			raise ValueError("Missing or invalid operation type")

		payload = operation.get("payload")
		if not isinstance(payload, dict):
			raise ValueError("Missing or invalid operation payload")

		handler = self.handlers.get(op_type)
		if handler is None:
			raise ValueError(f"Unsupported operation: {op_type}")

		handler.validate(payload)
		return handler.execute(payload, sudo_password=sudo_password)
