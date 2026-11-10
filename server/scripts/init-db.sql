-- Schema for teaching-server-management 

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	first_name TEXT NOT NULL,
	last_name TEXT NOT NULL,
	email TEXT NOT NULL UNIQUE,
	password_hash TEXT NOT NULL,
	role TEXT NOT NULL,
	username TEXT NOT NULL UNIQUE,
	status TEXT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS courses (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	teacher_id UUID NOT NULL REFERENCES users (id),
	name TEXT NOT NULL,
	description TEXT,
	academic_year TEXT NOT NULL,
	archived_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS enrollments (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	course_id UUID NOT NULL REFERENCES courses (id),
	student_id UUID NOT NULL REFERENCES users (id),
	UNIQUE (course_id, student_id)
);

CREATE TABLE IF NOT EXISTS projects (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	course_id UUID NOT NULL REFERENCES courses (id),
	name TEXT NOT NULL,
	description TEXT,
	type TEXT NOT NULL,
	deadline TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS teams (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	project_id UUID NOT NULL REFERENCES projects (id),
	name TEXT NOT NULL,
	storage_path TEXT NOT NULL,
	UNIQUE (project_id, name)
);

CREATE TABLE IF NOT EXISTS team_members (
	team_id UUID NOT NULL REFERENCES teams (id),
	student_id UUID NOT NULL REFERENCES users (id),
	PRIMARY KEY (team_id, student_id)
);

CREATE TABLE IF NOT EXISTS submissions (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	project_id UUID NOT NULL REFERENCES projects (id),
	student_id UUID REFERENCES users (id),
	team_id UUID REFERENCES teams (id),
	storage_path TEXT NOT NULL,
	submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	CHECK (student_id IS NOT NULL OR team_id IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS server_operations (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	type TEXT NOT NULL,
	payload JSONB NOT NULL DEFAULT '{}'::jsonb,
	status TEXT NOT NULL,
	requested_by UUID NOT NULL REFERENCES users (id),
	approved_by UUID REFERENCES users (id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	approved_at TIMESTAMPTZ,
	executed_at TIMESTAMPTZ,
	execution_result TEXT
);

CREATE TABLE IF NOT EXISTS audit_logs (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES users (id),
	action TEXT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_courses_teacher_id ON courses (teacher_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON enrollments (student_id);
CREATE INDEX IF NOT EXISTS idx_projects_course_id ON projects (course_id);
CREATE INDEX IF NOT EXISTS idx_teams_project_id ON teams (project_id);
CREATE INDEX IF NOT EXISTS idx_team_members_student_id ON team_members (student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_project_id ON submissions (project_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student_id ON submissions (student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_team_id ON submissions (team_id);
CREATE INDEX IF NOT EXISTS idx_server_operations_requested_by ON server_operations (requested_by);
CREATE INDEX IF NOT EXISTS idx_server_operations_approved_by ON server_operations (approved_by);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id);
