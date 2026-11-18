"""Client configuration knobs."""

from __future__ import annotations

import re

# API / CLI
DEFAULT_BASE_URL = "http://localhost:8080"
API_TIMEOUT_SECONDS = 10.0
APPROVED_OPERATION_STATUS = "APPROVED"
# Matches server OperationExecutionFeedbackRequest @Size(max = 1000).
EXECUTION_RESULT_MAX_LEN = 1000

# Linux group naming.
GROUP_NAME_MAX_LEN = 32
GROUP_NAME_PATTERN = re.compile(rf"^[a-z_][a-z0-9_-]{{0,{GROUP_NAME_MAX_LEN - 1}}}$")
GROUP_NAME_DIGIT_PREFIX = "c-"

# Subprocess timeouts (seconds).
GROUP_LOOKUP_TIMEOUT = 10
GROUP_COMMAND_TIMEOUT = 30

# Absolute paths used by LinuxGroupManager.
GROUPADD = "/usr/sbin/groupadd"
GROUPDEL = "/usr/sbin/groupdel"
GROUPMOD = "/usr/sbin/groupmod"
GETENT = "/usr/bin/getent"
SUDO = "sudo"
