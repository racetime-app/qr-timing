# RaceTime QR Timing

Self check-in for [RaceTime](https://racetime.app) events. Instead of putting staff at every checkpoint, the
organiser puts up a QR code. Athletes scan it with their phone, enter their bib number (and a PIN, if they were
given one) and the check-in is recorded in RaceTime.

This repository is also a **worked example of a RaceTime API integration**. It is a small, plain Spring Boot app,
written to be read: start with [`CheckInService`](src/main/java/app/racetime/qrtiming/checkin/CheckInService.java)
for the flow and [`HttpRaceTimeApiClient`](src/main/java/app/racetime/qrtiming/api/HttpRaceTimeApiClient.java) for
the API calls.

## How it works

```
 Athlete's phone           QR Timing (this app)                       RaceTime API
 ───────────────           ────────────────────                       ────────────
 scans QR code ────▶ GET  /c/{eventId}/{checkpointId}
                             ├─ GET /api/v1/events/{eventId} ──────────────────▶ event, timing method
                             └─ GET /api/v1/events/{eventId}/checkpoints ──────▶ course
               ◀──── check-in form

 bib + PIN ────────▶ POST /c/{eventId}/{checkpointId}
                             ├─ (event and checkpoints, as above)
                             ├─ GET  /api/v1/events/{eventId}/participants?bib= ▶ athletes and their PINs
                             └─ POST /api/v1/events/{eventId}/checkIn ─────────▶ records the time
               ◀──── confirmation, or what went wrong
```

* QR Timing uses only the **public RaceTime API**, the same endpoints any integration can call. It authenticates
  with a RaceTime API token that belongs to an admin (in case of existing service that serve everybody) or race owner.
* The QR code holds the event ID and the checkpoint ID, and nothing secret. RaceTime runs this app at
  `https://qr.racetime.app`, and the RaceTime app prints the codes for it from an event's Route page.
* Organisers hand an event's timing to an integration by setting its timing method to `INTEGRATION` (checkpoints
  and finish) or `INTEGRATION_CHECKPOINTS` (checkpoints only, timekeepers at the finish). QR Timing refuses
  check-ins for events left to timekeepers (`MANUAL`).
* Bib numbers may repeat within an event, and PINs are optional. QR Timing picks the athlete from the bib number
  and the PIN, and limits PIN guessing.
* The time recorded is the moment the athlete's form arrives. RaceTime checks the course order: a missed checkpoint
  is refused, and a repeated check-in shows the first time again.
* The pages are rendered on the server with FreeMarker and use no JavaScript.

Each API call and how its answers are handled is described in [docs/racetime-api.md](docs/racetime-api.md).

## Pages

| Page | Address | Template |
|---|---|---|
| Landing: what QR Timing is | `/` | [`landing.ftlh`](src/main/resources/templates/landing.ftlh) |
| Check-in form | `/c/{eventId}/{checkpointId}` | [`check-in.ftlh`](src/main/resources/templates/check-in.ftlh) |
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
| Checkpoint 1, "Windmill turn" | http://localhost:8080/c/demo/windmill |
| Checkpoint 2, without a name | http://localhost:8080/c/demo/cp2 |
| Finish | http://localhost:8080/c/demo/finish |
| Out-of-date code (event timed manually) | http://localhost:8080/c/manual/finish |

And try these bib numbers:

| Bib | Athlete |
|---|---|
| `214`, PIN `4821` | Has started. Leave out the PIN or enter another one to see the PIN errors; five wrong PINs block the bib for 15 minutes. |
| `100`, no PIN | Has started. Any PIN, or none, is accepted. |
| `777` | Two athletes share this bib, with PINs `1111` and `2222`. Without a PIN they can't be told apart. |
| `400` | Has not started. |
| `503` | RaceTime is not responding. |
| anything else | Bib number not found. |

The demo follows the course order like RaceTime does. Check in at the finish before `windmill` to see "A checkpoint
was missed". Check in twice at the same checkpoint to see the confirmation again.

Without the `demo` profile the app calls the real API at `https://api.racetime.app` and needs an API token:

```bash
RACETIME_API_TOKEN=<token> mvn spring-boot:run
```

## Tests

```bash
mvn test
```

* `HttpRaceTimeApiClientTest`: the HTTP requests, and how each kind of API response is handled.
* `ParticipantMatcherTest`: which athlete a bib number and PIN point to.
* `CheckInServiceTest`: the check-in flow against a mocked API.
* `CheckInControllerTest`: every page, rendered with the real templates.
* `QrTimingApplicationTests`: the whole app with the demo API.

## Configuration

Settings are in [`application.yaml`](src/main/resources/application.yaml). Each can be overridden with an
environment variable:

| Setting | Environment variable | Default |
|---|---|---|
| `racetime.api.base-url` | `RACETIME_API_BASE_URL` | `https://api.racetime.app` |
| `racetime.api.token` | `RACETIME_API_TOKEN` | none: required unless the `demo` profile is active |
| `racetime.api.connect-timeout` | `RACETIME_API_CONNECT_TIMEOUT` | `3s` |
| `racetime.api.read-timeout` | `RACETIME_API_READ_TIMEOUT` | `10s` |
| `qr-timing.racetime-app-url` | `QR_TIMING_RACETIME_APP_URL` | `https://racetime.app/get-app?utm_source=qr_timing` |
| `server.port` | `PORT` | `8080` |

## Deploy to Google Cloud Run

The [`Dockerfile`](Dockerfile) builds the app and runs it on a JRE image. Setting it up takes four steps, and only
the last one is repeated for each new version.

**1. Store the API token in Secret Manager.**

```bash
printf '%s' "<token>" | gcloud secrets create qr-timing-api-token --project PROJECT_ID --data-file=-
```

**2. Create a service account for the app.** Cloud Run runs every service as some service account. Unless you name
one, that is the project's default compute account, which many other services share and which usually holds the
broad Editor role. An admin token is powerful, so give QR Timing an account of its own that can read this one secret
and nothing else:

```bash
gcloud iam service-accounts create qr-timing --project PROJECT_ID --display-name "QR Timing"
```

The account needs no other roles. QR Timing calls only the RaceTime API, and it authenticates there with the
token, not with Google credentials.

**3. Let the account read the secret.** Grant the role on the secret itself, not on the whole project:

```bash
gcloud secrets add-iam-policy-binding qr-timing-api-token \
  --project PROJECT_ID \
  --member serviceAccount:qr-timing@PROJECT_ID.iam.gserviceaccount.com \
  --role roles/secretmanager.secretAccessor
```

**4. Build and deploy.** Cloud Run builds the image from the source, runs it as the new account and hands it the
token as `RACETIME_API_TOKEN`:

```bash
gcloud run deploy qr-timing \
  --source . \
  --project PROJECT_ID \
  --region us-central1 \
  --allow-unauthenticated \
  --service-account qr-timing@PROJECT_ID.iam.gserviceaccount.com \
  --set-secrets RACETIME_API_TOKEN=qr-timing-api-token:latest
```

If the deployment fails:

* *"Permission denied on secret ... for Revision service account ..."*: the account the service runs as can't read
  the secret. Check that `--service-account` names the account from step 2 and that step 3 was done. Without
  `--service-account`, the error names the default compute account (`...-compute@developer.gserviceaccount.com`).
  A new grant can take a minute to apply, so you may need to try again.
* *"... does not have permission to act as service account ..."*: deploying a service that runs as another account
  needs the `Service Account User` role (`roles/iam.serviceAccountUser`) on that account. Project owners have it.

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
  checkin/   the check-in flow: finding the athlete, PIN rules, attempt limit
  config/    settings, the RestClient set-up, security headers
  web/       controllers, the check-in form, error page variants
src/main/resources/
  templates/ FreeMarker pages (layout.ftlh holds the shared shell)
  static/    CSS and images
  docs/      the RaceTime API calls the app makes
```

## License

[Apache License 2.0](LICENSE)
