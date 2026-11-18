"""Minimal raw-terminal key reader (no scrollback prompts)."""

from __future__ import annotations

import select
import sys
import termios
import tty


def read_key() -> str:
	"""Read one keypress. Arrow keys become 'up' / 'down' / 'left' / 'right'."""
	fd = sys.stdin.fileno()
	old = termios.tcgetattr(fd)
	try:
		tty.setraw(fd)
		ch = sys.stdin.read(1)
		if ch == "\x1b":
			if select.select([sys.stdin], [], [], 0.05)[0]:
				nxt = sys.stdin.read(1)
				if nxt == "[" and select.select([sys.stdin], [], [], 0.05)[0]:
					code = sys.stdin.read(1)
					return {
						"A": "up",
						"B": "down",
						"C": "right",
						"D": "left",
					}.get(code, "esc")
			return "esc"
		if ch in ("\r", "\n"):
			return "enter"
		if ch == "\x03":
			return "ctrl-c"
		if ch == "\x7f":
			return "backspace"
		return ch
	finally:
		termios.tcsetattr(fd, termios.TCSADRAIN, old)
