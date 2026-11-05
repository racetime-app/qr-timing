# RaceTime QR Timing

Self check-in for [RaceTime](https://racetime.app) events. Instead of putting staff at every checkpoint, the
organiser puts up a QR code. Athletes scan it with their phone, enter their bib number (and a PIN, if they were
given one) and the check-in is recorded in RaceTime.

This repository is also a **worked example of a RaceTime API integration**. It is a small, plain Spring Boot app,
written to be read: start with [`HttpRaceTimeApiClient`](src/main/java/app/racetime/qrtiming/api/HttpRaceTimeApiClient.java).

## How it works

```
 Athlete's phone                 QR Timing (this app)                    RaceTime API
 ───────────────                 ────────────────────                    ────────────
 scans QR code ──────────▶ GET  /c/{eventId}/{checkpointId}/{key}
                                   └─ GET  /api/v1/qr/{eventId}/{checkpointId}/{key} ──▶ event + checkpoint
               ◀────────── check-in form

 sends bib + PIN ────────▶ POST /c/{eventId}/{checkpointId}/{key}
                                   └─ POST /api/v1/qr/{…}/checkIn {bib, pin} ─────────▶ records the time
               ◀────────── confirmation, or what went wrong
```

* The QR code holds a link to this app. Its last part, the `key`, is a secret that proves the athlete scanned the
  real code. It is all the API needs to accept a check-in, so **the app holds no RaceTime credentials**.
* The API records the time itself, when the check-in arrives.
* PINs are optional. If an athlete has one, it must be entered. If not, the PIN field is simply left empty.
* The pages are server-rendered with FreeMarker and use no JavaScript.

The API calls are described in [docs/api-contract.md](docs/api-contract.md).

## Pages

| Page | Address | Template |
|---|---|---|
| Landing: what QR Timing is | `/` | [`landing.ftlh`](src/main/resources/templates/landing.ftlh) |
| Check-in form | `/c/{eventId}/{checkpointId}/{key}` | [`check-in.ftlh`](src/main/resources/templates/check-in.ftlh) |
| Confirmation | after sending the form | [`success.ftlh`](src/main/resources/templates/success.ftlh) |
| Errors: wrong PIN, unknown bib, out-of-date code, missed checkpoint, ... | | [`error.ftlh`](src/main/resources/templates/error.ftlh) |

Every page works on phones and on desktop screens. All texts are in
[`messages.properties`](src/main/resources/messages.properties).

## Run it locally

You need JDK 21 and Maven 3.9.

The `demo` profile replaces the RaceTime API with an in-memory fake, so you can try every page without an account:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Then open one of these:

| QR code | Link |
|---|---|
| Named checkpoint | http://localhost:8080/c/demo/windmill/demo-key |
| Checkpoint without a name | http://localhost:8080/c/demo/cp3/demo-key |
| Finish | http://localhost:8080/c/demo/finish/demo-key |
| Out-of-date code | http://localhost:8080/c/demo/finish/old-key |

And try these bib numbers:

| Bib | Result |
|---|---|
| `214`, PIN `4821` | Checked in. Leave out the PIN or enter another one to see the PIN errors. |
| `100`, no PIN | Checked in, whatever you type as PIN. |
| `300` | A checkpoint was missed. |
| `400` | Start not recorded. |
| `500` | Too many attempts. |
| `503` | RaceTime is not responding. |
| anything else | Bib number not found. |

Without the `demo` profile the app calls the real API at `https://api.racetime.app`.

## Tests

```bash
mvn test
```

* `HttpRaceTimeApiClientTest`: the HTTP requests, and how each kind of API response is handled.
* `CheckInControllerTest`: every page, rendered with the real templates against a mocked API.
* `QrTimingApplicationTests`: the whole app with the demo API.

## Configuration

Settings are in [`application.yaml`](src/main/resources/application.yaml). Each can be overridden with an
environment variable:

| Setting | Environment variable | Default |
|---|---|---|
| `racetime.api.base-url` | `RACETIME_API_BASE_URL` | `https://api.racetime.app` |
| `racetime.api.connect-timeout` | `RACETIME_API_CONNECT_TIMEOUT` | `3s` |
| `racetime.api.read-timeout` | `RACETIME_API_READ_TIMEOUT` | `10s` |
| `qr-timing.racetime-app-url` | `QR_TIMING_RACETIME_APP_URL` | `https://racetime.app/get-app?utm_source=qr_timing` |
| `server.port` | `PORT` | `8080` |

## Deploy to Google Cloud Run

The [`Dockerfile`](Dockerfile) builds the app and runs it on a JRE image. Cloud Run can build it for you:

```bash
gcloud run deploy qr-timing \
  --source . \
  --project PROJECT_ID \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars RACETIME_API_BASE_URL=https://api.racetime.app
```

`/actuator/health` answers `{"status":"UP"}` and can serve as the startup and liveness probe.

To try the container locally:

```bash
docker build -t qr-timing .
```

```bash
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=demo qr-timing
```

## Project layout

```
src/main/java/app/racetime/qrtiming/
  api/       the RaceTime API: client interface, HTTP client, demo client, response records
  config/    settings, the RestClient set-up, security headers
  web/       controllers, the check-in form, error page variants
src/main/resources/
  templates/ FreeMarker pages (layout.ftlh holds the shared shell)
  static/    CSS and images
docs/        the API contract
```

## License

[Apache License 2.0](LICENSE)
