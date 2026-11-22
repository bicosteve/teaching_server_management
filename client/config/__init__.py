"""Client configuration knobs."""

from __future__ import annotations

import re

# API / CLI
DEFAULT_BASE_URL = "http://localhost:8080"
API_TIMEOUT_SECONDS = 10.0
APPROVED_OPERATION_STATUS = "APPROVED"
# Matches server OperationExecutionFeedbackRequest @Size(max = 1000).
EXECUTION_RESULT_MAX_LEN = 1000

# Shared Linux group name constraints.
LINUX_GROUP_NAME_MAX_LEN = 32
LINUX_GROUP_NAME_PATTERN = re.compile(
	rf"^[a-z_][a-z0-9_-]{{0,{LINUX_GROUP_NAME_MAX_LEN - 1}}}$"
)

# Course Linux group naming
COURSE_GROUP_DIGIT_PREFIX = "c-"

# Course / project file tree root on the teaching server.
FILES_ROOT = "/srv/teaching-server-management"

# Subprocess timeouts (seconds).
GROUP_LOOKUP_TIMEOUT = 10
COMMAND_TIMEOUT = 30

# Absolute paths used by LinuxGroupManager.
GROUPADD = "/usr/sbin/groupadd"
GROUPDEL = "/usr/sbin/groupdel"
GROUPMOD = "/usr/sbin/groupmod"
GETENT = "/usr/bin/getent"
SUDO = "sudo"

# Absolute paths used by CourseDirectoryManager.
MKDIR = "/bin/mkdir"
MV = "/bin/mv"
RM = "/bin/rm"
RMDIR = "/bin/rmdir"
CHOWN = "/bin/chown"
CHMOD = "/bin/chmod"
