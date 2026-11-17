-- Seed three teacher courses (PENDING / SYNCED / FAILED) for local development.
-- Requires the teacher user from seed-users.sql.

DO $$
DECLARE
	teacher_id UUID;
	pending_id UUID;
	synced_id UUID;
	failed_id UUID;
	op_id UUID;
BEGIN
	SELECT id INTO teacher_id FROM users WHERE username = 'teacher';
	IF teacher_id IS NULL THEN
		RAISE EXCEPTION 'Teacher user not found; run seed-users.sh first';
	END IF;

	INSERT INTO courses (teacher_id, name, description, academic_year, status)
	VALUES
		(teacher_id, 'Algorithms I', 'Introductory algorithms course', '2025/2026', 'PENDING'),
		(teacher_id, 'Operating Systems', 'Processes, memory, and concurrency', '2025/2026', 'SYNCED'),
		(teacher_id, 'Databases', 'Relational modeling and SQL', '2025/2026', 'FAILED')
	ON CONFLICT (name, academic_year) DO NOTHING;

	SELECT id INTO pending_id FROM courses WHERE name = 'Algorithms I' AND academic_year = '2025/2026';
	SELECT id INTO synced_id FROM courses WHERE name = 'Operating Systems' AND academic_year = '2025/2026';
	SELECT id INTO failed_id FROM courses WHERE name = 'Databases' AND academic_year = '2025/2026';

	IF pending_id IS NOT NULL AND NOT EXISTS (
		SELECT 1 FROM server_operation_courses WHERE course_id = pending_id
	) THEN
		INSERT INTO server_operations (type, payload, status, requested_by)
		VALUES (
			'CREATE_COURSE',
			jsonb_build_object(
				'name', 'Algorithms I',
				'description', 'Introductory algorithms course',
				'academic_year', '2025/2026'
			),
			'PENDING',
			teacher_id
		)
		RETURNING id INTO op_id;
		INSERT INTO server_operation_courses (operation_id, course_id) VALUES (op_id, pending_id);
	END IF;

	IF synced_id IS NOT NULL AND NOT EXISTS (
		SELECT 1 FROM server_operation_courses WHERE course_id = synced_id
	) THEN
		INSERT INTO server_operations (
			type, payload, status, requested_by, approved_by, approved_at, executed_at, execution_result
		)
		VALUES (
			'CREATE_COURSE',
			jsonb_build_object(
				'name', 'Operating Systems',
				'description', 'Processes, memory, and concurrency',
				'academic_year', '2025/2026'
			),
			'COMPLETED',
			teacher_id,
			teacher_id,
			now(),
			now(),
			'Course workspace provisioned'
		)
		RETURNING id INTO op_id;
		INSERT INTO server_operation_courses (operation_id, course_id) VALUES (op_id, synced_id);
	END IF;

	IF failed_id IS NOT NULL AND NOT EXISTS (
		SELECT 1 FROM server_operation_courses WHERE course_id = failed_id
	) THEN
		INSERT INTO server_operations (
			type, payload, status, requested_by, approved_by, approved_at, executed_at, execution_result
		)
		VALUES (
			'CREATE_COURSE',
			jsonb_build_object(
				'name', 'Databases',
				'description', 'Relational modeling and SQL',
				'academic_year', '2025/2026'
			),
			'FAILED',
			teacher_id,
			teacher_id,
			now(),
			now(),
			'Provisioning failed'
		)
		RETURNING id INTO op_id;
		INSERT INTO server_operation_courses (operation_id, course_id) VALUES (op_id, failed_id);
	END IF;
END $$;
