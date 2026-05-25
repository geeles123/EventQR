# EventQR

## Overview

EventQR is a QR-based event access and attendee transaction management system. It provides attendee-facing flows (browse events, register, view QR credentials, transaction history, rewards, notifications), staff flows (manual QR scanning, record transactions, lookup registrations, ID printing), and organizer/admin flows (event management, user/role management, scan purposes, rewards, reports, notifications). The Android app talks to a Spring Boot backend via a REST API.

Live backend API base URL used by the Android app:

```
https://eventqr-backend-owoa.onrender.com/api/v1/
```

## Repository Structure

Top-level layout (canonical):

```
EVENTQR/
├── .github/
├── EventQRBackend/
│   └── eventqr/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/thedavelopers/eventqr/
│       │   │   │   ├── EventqrApplication.java
│       │   │   │   ├── features/
│       │   │   │   └── shared/
│       │   │   └── resources/
│       │   └── test/
│       ├── pom.xml
│       ├── Dockerfile
│       ├── .dockerignore
│       ├── mvnw
│       └── mvnw.cmd
└── EventQRMobile/
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle.properties
    ├── gradlew
    ├── gradlew.bat
    └── app/
        ├── build.gradle.kts
        ├── proguard-rules.pro
        └── src/
            ├── androidTest/
            ├── main/
            │   ├── java/com/thedavelopers/eventqr/
            │   │   ├── core/
            │   │   ├── features/
            │   │   ├── Landing.kt
            │   │   ├── SignIn.kt
            │   │   ├── Registration.kt
            │   │   ├── Dashboard.kt
            │   │   ├── ForgotPassword.kt
            │   │   └── ChangePassword.kt
            │   └── res/
            └── test/
```

Note: `EventQRBackend/eventqr/src/main` and `EventQRMobile/app/src/main` are separate project source trees that happen to share the `src/main` convention; do NOT merge or move them.

## Important Folder Explanations

- `.github/` — CI/workflow files (do not delete unless intended).
- `EventQRBackend/eventqr/` — Spring Boot backend module (Maven). Contains `pom.xml`, Dockerfile, and `src/` with Java server code.
- `EventQRBackend/eventqr/src/main/java/com/thedavelopers/eventqr/features/` — Feature-based domain modules (controllers, services, repositories).
- `EventQRBackend/eventqr/src/main/java/com/thedavelopers/eventqr/shared/` — Shared utilities, exceptions, DTOs, constants used across features.
- `EventQRBackend/eventqr/src/main/resources/` — Spring resources and configuration (application properties, static files).
- `EventQRMobile/` — Android project root. Gradle wrapper, settings, and subproject `app/`.
- `EventQRMobile/app/src/main/java/com/thedavelopers/eventqr/core/` — Core helpers: API client, session management, common utilities.
- `EventQRMobile/app/src/main/java/com/thedavelopers/eventqr/features/` — Screen-specific packages and presenters.
- `EventQRMobile/app/src/main/res/layout/` — XML layouts (protected UI resources).

## Backend Architecture

- Spring Boot application (module at `EventQRBackend/eventqr/`).
- Feature-based modular organization: `features/` for domain logic, `shared/` for common code.
- Configuration via environment variables for database/Supabase and Render deployment.
- Exposes REST API endpoints under `/api/v1/` consumed by the Android app.
- Dockerfile present at `EventQRBackend/eventqr/Dockerfile` used for containerized deployment.

## Android Architecture

- Kotlin Android app (classic View + XML layouts). The project uses an MVP-like pattern:
  - `Activity` / `Fragment` act as the View.
  - Presenters handle form validation and view logic.
  - Repositories / API client handle network I/O and data mapping.
- `core/` contains `ApiConfig` / `ApiClient` and session helpers.
- `features/` contains screen-specific flows (events, registrations, transactions, scanner, admin screens).
- QR generation uses ZXing core; camera scanning is intentionally deferred (manual input supported).
- The app calls the backend API via Retrofit; the base URL is defined in an app API config (e.g., `ApiConfig`).

## Backend ↔ Mobile Relationship

- Backend and Mobile are separate sibling projects at the repository root. Do NOT move files between them.
- The Android app talks to the backend REST API (not directly to Supabase). Backend handles database/auth.

## Implemented Features (current)

- Attendee: event browsing, event details, registration flows, QR credential lookup/generation (display), transaction history, rewards, notifications.
- Staff: manual QR scanning workflow, transaction recording, event registration lookup, ID printing foundations.
- Organizer/Admin: event creation/management, user/role management, scan purpose management, rewards management, reports, notification management.

## Known TODOs / Limitations

- Attendee-specific registered-events lookup requires a backend user-scoped registrations endpoint (missing).
- Forgot-password and change-password have UI but no backend endpoints yet.
- Camera-based QR scanning is deferred; manual QR input remains supported.

## Build & Run

Backend (run locally with Maven wrapper):

```bash
cd EventQRBackend/eventqr
./mvnw spring-boot:run       # Unix/macOS
# Windows PowerShell
# mvnw.cmd spring-boot:run
```

Docker (build & run):

```bash
cd EventQRBackend/eventqr
docker build -t eventqr-backend .
docker run --rm -p 10000:10000 -e PORT=10000 eventqr-backend
```

Android (assemble debug):

```bash
cd EventQRMobile
./gradlew assembleDebug      # Unix/macOS
# Windows
# gradlew.bat assembleDebug
```

Open in Android Studio: open the `EventQRMobile/` folder and let Gradle sync complete.

## Where the API base URL lives

The Android app defines the backend base URL in an API config class (e.g., `ApiConfig`). Example:

```
https://eventqr-backend-owoa.onrender.com/api/v1/
```

Ensure any code changes preserve that value or make it configurable via `gradle.properties` only (do not commit secrets).

## Environment & Secrets

- Do NOT commit `.env`, `application-*` files containing secrets, database passwords, Supabase keys, JWT secrets, or Render environment variables.
- Backend secrets belong in Render / Supabase environment configuration, not in the repo.
- Android should only contain the public API base URL; do not embed private keys.

## Rules for Future AI Agents (safety-first)

1. Do NOT move backend files into `EventQRMobile/` or vice versa. `EventQRBackend/` and `EventQRMobile/` are siblings.
2. Do NOT recreate or duplicate the backend. The canonical backend is `EventQRBackend/eventqr/`.
3. Do not modify backend or Android source outside the requested task scope.
4. Do NOT delete `src/`, `res/`, `features/`, or `core/` folders or protected XML layouts without human approval.
5. Do NOT switch Android UI to Jetpack Compose or change the app namespace/applicationId.
6. Do NOT connect Android directly to Supabase for core business logic; backend mediates DB access.
7. Do NOT commit or push changes unless explicitly instructed.
8. Always run the relevant build before proposing mergeable changes:
   - Backend: `cd EventQRBackend/eventqr && ./mvnw spring-boot:run`
   - Android: `cd EventQRMobile && ./gradlew assembleDebug`
9. Preserve MVP pattern on Android and feature-modular structure on the backend.
10. If you find unexpected `pom.xml`, `mvnw`, `Dockerfile`, or `EventQRBackend` inside `EventQRMobile`, STOP and report — do not delete without human approval.

## Recommended Workflow for Changes

1. Determine whether change affects backend or mobile.
2. Work only in the correct project folder.
3. Inspect existing feature package and keep naming/structure consistent.
4. Reuse existing API client/session code and adapters.
5. Build the affected project locally and report results.
6. Present a short changelist and test results for review.

## Verification & Notes

- This README only updates `README.md` at the repository root.
I inspected the repository structure to populate the tree; if any folders are missing from the listing, tell me which path to inspect and I will update the README.