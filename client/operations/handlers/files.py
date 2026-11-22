"""Course directory helpers for course operation handlers."""

from __future__ import annotations

from pathlib import Path

from config import CHMOD, CHOWN, FILES_ROOT, MKDIR, MV, RM, RMDIR
from operations.handlers.groups import slugify
from operations.shell import run_sudo

FILE_PERMISSION = {
	"COURSE_GROUP_ONLY": "2770", 
}


def course_directory_path(name: str, academic_year: str) -> Path:
	course = slugify(name)
	year = slugify(academic_year)
	if not course or not year:
		raise ValueError("Cannot derive course path from empty name or academic_year")
	return Path(FILES_ROOT) / year / course


def require_under_files_root(path: Path) -> Path:
	"""Reject paths outside FILES_ROOT or not exactly {{year}}/{{course}}."""
	root = Path(FILES_ROOT)
	try:
		relative = path.resolve(strict=False).relative_to(root.resolve(strict=False))
	except ValueError as exc:
		raise ValueError(f"Path must be under {FILES_ROOT}: {path}") from exc
	if len(relative.parts) != 2:
		raise ValueError(f"Path must be {{year}}/{{course}} under {FILES_ROOT}: {path}")
	return path


class CourseDirectoryManager:
	"""Create, rename, and delete course directories under FILES_ROOT."""

	def create(self, path: Path, *, group_name: str, sudo_password: str) -> dict:
		path = require_under_files_root(path)
		if path.is_dir():
			raise RuntimeError(f"Directory already exists: {path}")
		run_sudo([MKDIR, "-p", str(path)], sudo_password)
		self._secure(path, group_name, sudo_password)
		return {"action": "CREATE", "path": str(path), "message": f"Directory {path} created successfully"}

	def delete(self, path: Path, *, sudo_password: str) -> dict:
		path = require_under_files_root(path)
		if not path.is_dir():
			raise RuntimeError(f"Directory does not exist: {path}")
		run_sudo([RM, "-rf", str(path)], sudo_password)
		self._rmdir_if_empty(path.parent, sudo_password)
		return {"action": "DELETE", "path": str(path), "message": f"Directory {path} deleted successfully"}

	def rename(
		self,
		old_path: Path,
		new_path: Path,
		*,
		group_name: str,
		sudo_password: str,
	) -> dict:
		old_path = require_under_files_root(old_path)
		new_path = require_under_files_root(new_path)
		if old_path == new_path:
			return {
				"action": "RENAME",
				"path": str(new_path),
				"previous_path": str(old_path),
				"message": f"Directory {old_path} already has the requested path",
			}
		if not old_path.is_dir():
			raise RuntimeError(f"Directory does not exist: {old_path}")
		if new_path.is_dir():
			raise RuntimeError(f"Directory already exists: {new_path}")
		run_sudo([MKDIR, "-p", str(new_path.parent)], sudo_password)
		run_sudo([MV, str(old_path), str(new_path)], sudo_password)
		self._secure(new_path, group_name, sudo_password)
		self._rmdir_if_empty(old_path.parent, sudo_password)
		return {
			"action": "RENAME",
			"path": str(new_path),
			"previous_path": str(old_path),
			"message": f"Directory {old_path} renamed to {new_path}",
		}

	@staticmethod
	def _secure(path: Path, group_name: str, sudo_password: str) -> None:
		run_sudo([CHOWN, f"root:{group_name}", str(path)], sudo_password)
		run_sudo([CHMOD, FILE_PERMISSION["COURSE_GROUP_ONLY"], str(path)], sudo_password)

	@staticmethod
	def _rmdir_if_empty(year_dir: Path, sudo_password: str) -> None:
		root = Path(FILES_ROOT)
		try:
			year_dir.resolve(strict=False).relative_to(root.resolve(strict=False))
		except ValueError:
			return
		if year_dir == root or not year_dir.is_dir() or any(year_dir.iterdir()):
			return
		run_sudo([RMDIR, str(year_dir)], sudo_password)
