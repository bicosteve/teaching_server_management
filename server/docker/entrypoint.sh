#!/bin/sh
set -eu

case "${MODE}" in
	dev)
		export SPRING_THYMELEAF_CACHE="${SPRING_THYMELEAF_CACHE:-false}"

		mkdir -p target/classes
		# Must live on the classpath (see spring.devtools.restart.trigger-file docs).
		touch target/classes/.reloadtrigger

		recompile_and_reload() {
			# Debounce bursty editor save events.
			sleep 0.2
			echo "Hot reload: compiling..."
			if ./mvnw -o -q -DskipTests compile; then
				touch target/classes/.reloadtrigger
				echo "Hot reload: restart triggered"
			else
				echo "Hot reload: compile failed" >&2
			fi
		}

		(
			echo "Hot reload: watching src/"
			while inotifywait -r -e modify,create,delete,move src; do
				recompile_and_reload
			done
		) &

		exec ./mvnw -B spring-boot:run \
			-Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.trigger-file=.reloadtrigger -Dspring.devtools.restart.poll-interval=300ms -Dspring.devtools.restart.quiet-period=100ms"
		;;
	prod)
		# Package from mounted source so prod matches the same code as dev.
		echo "Packaging application..."
		./mvnw -B -DskipTests package
		jar="$(ls target/*.jar | grep -v '\.original$' | head -n 1)"
		exec java -jar "$jar"
		;;
	*)
		echo "MODE must be 'dev' or 'prod' (got: '${MODE:-}')" >&2
		exit 1
		;;
esac
