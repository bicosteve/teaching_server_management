from abc import ABC, abstractmethod
from typing import Any


class OperationHandler(ABC):

	@abstractmethod
	def validate(self, params: dict[str, Any]) -> None:
		pass

	@abstractmethod
	def execute(self, params: dict[str, Any], *, sudo_password: str) -> dict:
		pass
