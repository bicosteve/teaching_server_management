-- Seed test users for teaching-server-management
-- Password for all accounts: password
-- Hash generated with Spring Security BCrypt (strength 10).

INSERT INTO users (first_name, last_name, email, password_hash, role, username, status)
VALUES
	(
		'Alice',
		'Admin',
		'admin@example.com',
		'$2a$10$2dZnjMozGUOgol5rcK1jR.vYdtQX8aHKvn0jPnMguXnekclgMTayO',
		'ADMIN',
		'admin',
		'ACTIVE'
	),
	(
		'Tom',
		'Teacher',
		'teacher@example.com',
		'$2a$10$2dZnjMozGUOgol5rcK1jR.vYdtQX8aHKvn0jPnMguXnekclgMTayO',
		'TEACHER',
		'teacher',
		'ACTIVE'
	),
	(
		'Sam',
		'Student',
		'student@example.com',
		'$2a$10$2dZnjMozGUOgol5rcK1jR.vYdtQX8aHKvn0jPnMguXnekclgMTayO',
		'STUDENT',
		'student',
		'ACTIVE'
	)
ON CONFLICT (username) DO NOTHING;
