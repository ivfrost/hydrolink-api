# Hydrolink API

Hydrolink started with a real need: controlling the irrigation on a countryside property without
checking on it constantly. It grew into a full IoT platform where ESP32 firmware, a Spring Boot API,
and a React Native app each run a layer and control real hardware. This repository is the backend
that connects them. It registers devices, links them to user accounts, tracks pin configuration and
watering schedules, ingests live device status over AWS IoT, and orchestrates over-the-air firmware
updates.

The mobile app that talks to this API is a separate, public repository:

- [hydrolink-app](https://github.com/ivfrost/hydrolink-app) React Native companion app

The ESP32 firmware is closed source.

What works today:

- Secure user auth and device linking tied to a per-device secret
- Live device status ingested through AWS IoT, with user and device management from the mobile app
- Watering schedules per day, held here and pushed down to the device
- Over-the-air firmware updates triggered from an upload

## Stack

- Java 25, Spring Boot 4.1, Spring Modulith for bounded modules
- PostgreSQL 16 for data, Redis for cache, MinIO for firmware binaries and images
- AWS IoT Core as the single message bus; the self-hosted MQTT broker was retired
- JWT authentication with separate web and mobile flows (httpOnly refresh cookie for web, bearer token for native)
- Dockerized dev and production profiles, fail-fast configuration

## Architecture

Device status is moved from AWS IoT Core into a standard SQS queue; commands are published back to
devices with the AWS IoT data plane Publish API. There is no self-hosted broker and the API holds no
device certificate.

```mermaid
flowchart LR
    DEV[("ESP32 controller<br/>hydrolink-core · AWS Thing · X.509")]
    RULE["AWS IoT rule<br/>SELECT *, topic(2) AS deviceKey<br/>FROM 'hydro/+/status'"]
    SQS[("SQS<br/>hydrolink-device-status")]
    DP["AWS IoT data plane<br/>Publish · SigV4"]
    API["Hydrolink API · Spring Boot<br/>SQS ingest · CommandGateway
    (AwsCommandGateway)"]
    DATA[("PostgreSQL · Redis · MinIO")]
    APP["hydrolink-app<br/>React Native"]

    DEV -- "status (presence / pin_config / station state)<br/>MQTT over TLS" --> RULE
    RULE --> SQS
    SQS -- "poll consume" --> API
    API -- "upsert pin state" --> DATA
    API -- "IAM-signed publish" --> DP
    DP -- "hydro/KEY/command /announce" --> DEV
    APP -- "REST /v1" --> API
```

An IoT rule forwards only what its SQL selects, so it stamps `topic(2)` back as `deviceKey` on each
message before it reaches SQS. Status payloads are complete snapshots rather than deltas, and each
handler writes only the state its message carries (pin modes are upserted per device and pin,
unrecognized modes are skipped with a warning). Because SQS delivers at least once, duplicates land
on the same result, so there is no ordering dependency.

Outbound commands go through a `CommandGateway` interface implemented by `AwsCommandGateway`. The
gateway builds topics only as `hydro/{deviceKey}/command` and `hydro/{deviceKey}/announce` and
publishes each with IAM. The corresponding IoT policies restrict publish to those exact topic
shapes, so status and secret feeds are not writable by this backend.

## Modules

Spring Modulith keeps each domain behind a clean boundary, with cross-module access going through a
small published API.

| Module      | What it owns |
|-------------|--------------|
| `users`     | Accounts, roles, passwords, recovery, profile, device link/unlink |
| `tokens`    | JWT access & refresh tokens, recovery codes, persisted token lifecycle |
| `devices`   | Device records, ownership secrets, pins, provisioning, secrets rotation |
| `schedules` | Per-day watering schedules and time windows, pushed to devices |
| `storage`   | MinIO uploads and OTA firmware lifecycle with versioned releases |
| `config`    | Security, the command gateway seam, AWS/SQS and object-store wiring |

## Notable pieces

- **Device provisioning is two separate planes.** A device record is an application-side row,
  created through an internal endpoint when firmware boots and linked by an ownership secret. The
  AWS Thing is provisioned separately by the device via AWS claim provisioning; the two never share
  an identifier.
- **Secret rotation is two-phase.** Regenerating a device secret does not make it authoritative
  until a `secret_rotated` acknowledgement comes back from the device, so a failed rotation leaves
  the old secret valid.
- **Over-the-air firmware updates.** Firmware is uploaded to MinIO with a version and checksum,
  then announced to matching devices on the `/announce` topic. Install can be forced even when the
  device reports the same version.
- **Pin reporting.** Devices publish their pin configuration, and the API stores each pin mode by
  device and pin number, skipping unrecognized modes with a warning instead of guessing at them.
- **Schedules.** Per-day watering schedules with fixed or sensor-linked time windows. Upserting
  persists the schedule, publishes it to the device, and returns which time windows conflict with
  each other in the response.

## Run locally

Requirements: Docker and a JDK matching the Java target in `pom.xml`. The app has no built-in
defaults for its secrets, so copy `.env.example` to `.env` and fill in every value first.

```bash
cp .env.example .env   # fill everything, generation hints are in the file
./mvnw -Dspring-boot.run.profiles=dev spring-boot:run
```

The `dev` profile enables the Spring Boot Docker Compose starter, so Postgres, Redis, and MinIO
start on their own. Other useful settings:

- Seed an admin and two devices through the `seed.*` env vars
- Reset the Postgres schema before boot if you edited the single V1 migration and Flyway checksums
  no longer match
- Swagger UI is available at `/api-docs-ui` on the dev profile; stage and production disable it
  unless `SPRINGDOC_API_DOCS_ENABLED` is set to `true`

Profiles map to `dev`, `stage`, `prod`, and `test` (used by the integration test suite).

The AWS data plane client needs `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` from `.env`; without
them the `AwsCommandGateway` bean fails to build.

## API documentation

The router is grouped into two OpenAPI specs so internal endpoints never leak to consumers:

- `public`: everything under `/v1`, minus `/v1/internal/**`
- `internal`: only `/v1/internal/**`, used for device-record registration and pin reporting

Specs are served on `/api-docs.yaml/public` and `/api-docs.yaml/internal`. Production disables the
Swagger UI and docs automatically.

## Deployment

A multi-stage Dockerfile packages the app as a JRE runtime image running as a non-root user.
`docker-compose.prod.yml` composes the API with Postgres, Redis, and MinIO, each with a healthcheck,
and relies on environment variables for real values. For this project I run the full stack self-hosted
on Linux and deploy it with Coolify; TLS terminates on a reverse proxy in front of the app, and
`server.forward-headers-strategy=framework` is set for that. The AWS side of the infrastructure (rule,
SQS, IoT data plane) lives in the eu-west-1 region.

AWS credentials are configured through `spring.cloud.aws.credentials.access-key` /
`secret-key`, which the dev profile reads from `.env`. Today the data plane client always builds a
static credentials provider from those values; resolving credentials from the deployment role
instead is planned but not implemented. Credential handling is a known area of active work.

## License

Released under the
[Creative Commons Attribution-NonCommercial 4.0 International](LICENSE) license. Noncommercial use
is fine; any other use needs permission.
