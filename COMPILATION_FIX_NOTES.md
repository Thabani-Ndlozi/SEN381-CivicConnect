# CivicConnect Firebase/Brevo compile fix (2026-10-14)

Fixes reported Maven compilation errors (5) in the previous integration candidate.

- `backend/src/main/java/com/civicconnect/feedback/BrevoMailService.java`: changed Jackson 2 import `com.fasterxml.jackson.databind.ObjectMapper` to Spring Boot 4's Jackson 3 `tools.jackson.databind.ObjectMapper`.
- `backend/src/main/java/com/civicconnect/feedback/NotificationOutboxListener.java`: imported `org.springframework.transaction.event.TransactionalEventListener` and `org.springframework.transaction.event.TransactionPhase`.

No changes to migrations, database files, environment secrets, frontend, or Docker volumes. This is a SOURCE fix only; **full Maven/Docker build and runtime behavior remain unverified here**. Rebuild on your machine, inspect further errors if any.

To fix an already-extracted copy, replace the two Java files from this ZIP, or make exactly these import edits locally, then run:

`docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build api web`

Before migration/cutover, continue using `AUTH_MODE=legacy` and verify your database backup. Do not use `docker compose down -v`.
