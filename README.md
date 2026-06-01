# EventQR

EventQR is a QR-based event registration and transaction management system for academic, organizational, and community events. It includes an Android mobile app and a Spring Boot REST API backend.

The system helps event teams manage attendee registration, QR credentials, event entry/exit, attendance, booth/session tracking, benefit claims, reward redemption, organizer tools, and administrative review workflows.

## Project Status

This repository contains the capstone/project implementation of EventQR. It is intended for demonstration, development, and academic review.

Do not treat this repository as a production deployment package without first configuring your own environment variables, database, authentication settings, and deployment infrastructure.

## Main Features

### Attendee

- Browse available events
- View event details
- Register for events
- Receive and view QR credentials
- View registered events
- View transaction history
- View and redeem available rewards
- View claimed rewards and notifications
- Edit profile information and profile photo

### Staff

- Select assigned events
- Scan or manually enter attendee QR codes
- Select scan purposes such as entry, exit, booth visit, session attendance, benefit claim, and reward redemption
- Record event transactions
- View assigned events and transaction logs

### Organizer

- Request event creation
- Manage approved events
- Manage attendees
- Configure scan purposes
- Configure transaction rules, point rules, ID templates, rewards, and staff assignments
- View transaction logs and reports

### Admin

- Review event creation requests
- Manage users and roles
- Monitor events and audit logs
- Manage platform-level notifications and administrative workflows

## Repository Structure

```txt
EventQR/
├── EventQRBackend/
│   └── eventqr/
│       ├── src/main/java/com/thedavelopers/eventqr/
│       │   ├── features/
│       │   └── shared/
│       ├── src/main/resources/
│       ├── pom.xml
│       └── Dockerfile
└── EventQRMobile/
    ├── app/
    │   ├── src/main/java/com/thedavelopers/eventqr/
    │   │   ├── core/
    │   │   └── features/
    │   └── src/main/res/
    ├── build.gradle.kts
    └── settings.gradle.kts
```

## Architecture

### Backend

- Java Spring Boot REST API
- Maven project under `EventQRBackend/eventqr`
- Feature-based package structure
- Database access handled through backend services and repositories
- REST endpoints exposed under an API version path
- Dockerfile included for containerized deployment

### Mobile

- Native Android app written in Kotlin
- XML-based UI layouts
- MVP-like screen organization
- Retrofit-based API communication
- Session management handled inside the mobile core layer
- QR generation/scanning and event transaction flows handled through app features

## Backend and Mobile Relationship

The Android app communicates with the backend API. The mobile app should not directly access private database credentials or backend-only services. Business logic, persistence, and protected operations should remain on the backend.

## Local Development

### Backend

```bash
cd EventQRBackend/eventqr
./mvnw spring-boot:run
```

Windows:

```bat
cd EventQRBackend\eventqr
mvnw.cmd spring-boot:run
```

### Backend with Docker

```bash
cd EventQRBackend/eventqr
docker build -t eventqr-backend .
docker run --rm -p 10000:10000 eventqr-backend
```

### Android

```bash
cd EventQRMobile
./gradlew assembleDebug
```

Windows:

```bat
cd EventQRMobile
gradlew.bat assembleDebug
```

Open the `EventQRMobile/` folder in Android Studio to run the app on an emulator or physical device.

## Configuration

Before running the full system, configure your own local or hosted backend environment. Required values depend on your deployment setup, but typically include:

- Database connection settings
- Authentication/JWT settings
- File or image storage settings
- Backend server port
- Android backend API base URL

Do not commit real secrets, production URLs, database passwords, private API keys, service-role keys, signing keys, or local environment files.

Recommended approach:

- Keep sensitive backend values in environment variables.
- Keep local-only files out of version control.
- Use placeholder/example configuration files when documenting setup.
- Configure the Android API base URL for your own backend instance before building.

## Security Notes for Public Repository Use

This repository should not expose:

- Production database URLs or passwords
- Supabase service-role keys or private storage keys
- JWT secrets
- Render or hosting environment variables
- Personal access tokens
- Local keystore files
- Real user data, attendee data, QR values, or event transaction records

If any of those values are accidentally committed, rotate the affected credentials immediately and remove them from the repository history if necessary.

## Suggested Development Workflow

1. Identify whether a change belongs to the backend or mobile project.
2. Work inside the correct project folder.
3. Reuse existing feature packages, DTOs, services, repositories, and UI patterns.
4. Keep backend business logic on the backend.
5. Keep Android changes compatible with the existing XML/MVP-style structure.
6. Build the affected project before committing.
7. Use clear commit messages such as `feat:`, `fix:`, `docs:`, `refactor:`, or `chore:`.

## License

No license has been specified yet. Add a license file before allowing external reuse or redistribution.
