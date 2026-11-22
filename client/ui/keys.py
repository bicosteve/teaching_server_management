"""Minimal raw-terminal key reader (no scrollback prompts)."""

from __future__ import annotations

import os
import select
import sys
import termios
import tty
from collections.abc import Iterator
from contextlib import contextmanager

ARROWS = {"A": "up", "B": "down", "C": "right", "D": "left"}
WHEEL = {64: "up", 65: "down"}


@contextmanager
def mouse_tracking() -> Iterator[None]:
	"""Report the wheel as CSI (SGR) instead of raw bytes."""
	sys.stdout.write("\x1b[?1000h\x1b[?1006h")
	sys.stdout.flush()
	try:
		yield
	finally:
		sys.stdout.write("\x1b[?1000l\x1b[?1006l")
		sys.stdout.flush()


def read_key() -> str:
	"""Read one keypress. Arrows/wheel become up/down/left/right; noise is 'ignore'."""
	fd = sys.stdin.fileno()
	old = termios.tcgetattr(fd)
	try:
		tty.setraw(fd)

		def getch() -> str:
			data = os.read(fd, 1)
			return data.decode("latin-1") if data else ""

		def ready(timeout: float) -> bool:
			return bool(select.select([fd], [], [], timeout)[0])

		ch = getch()
		if ch != "\x1b":
			if ch in ("\r", "\n"):
				return "enter"
			if ch == "\x03":
				return "ctrl-c"
			if ch == "\x7f":
				return "backspace"
			return "ignore" if ch and ord(ch) < 32 else ch

		if not ready(0.05):
			return "esc"
		kind = getch()
		if kind == "O":
			return ARROWS.get(getch() if ready(0.05) else "", "esc")
		if kind != "[":
			return "esc"

		# CSI: read until a final byte (@..~). Also handles SGR mouse / X10.
		body = ""
		while len(body) < 64:
			if not ready(0.05):
				return "ignore"
			body += getch()
			if 0x40 <= ord(body[-1]) <= 0x7E:
				break

		final, params = body[-1], body[:-1]
		if final in ARROWS and not params.startswith("<"):
			return ARROWS[final]

		# X10 mouse: ESC [ M cb cx cy
		if final == "M" and not params:
			cb = getch() if ready(0.05) else ""
			if ready(0.05):
				getch()
			if ready(0.05):
				getch()
			button = (ord(cb) - 32) if cb else -1
			return WHEEL.get(button & ~0b11100, "ignore")

		# SGR mouse: ESC [ <btn ; col ; row M
		if final in "Mm":
			core = params[1:] if params.startswith("<") else params
			try:
				button = int(core.split(";", 1)[0])
			except ValueError:
				return "ignore"
			return WHEEL.get(button & ~0b11100, "ignore")

		return "ignore"
	finally:
		termios.tcsetattr(fd, termios.TCSADRAIN, old)
