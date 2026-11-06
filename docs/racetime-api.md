# RaceTime API calls made by QR Timing

QR Timing uses only the public RaceTime API (the
[Race Integration API](https://racetime.gitbook.io/docs/api/race-integration-api)). Every integration can use the same
endpoints, and QR Timing has no special access.

## The QR code

When an organiser leaves the timing of an event to an integration, the QR Timing operator puts a code at every
checkpoint (and the finish). It holds a link to this app:

```
https://<qr-timing-host>/c/{eventId}/{checkpointId}
```

`eventId` and `checkpointId` are the IDs RaceTime uses for the event and the checkpoint. Nothing in the link is
secret. Athletes prove who they are with their bib number and, if the organiser gave them one, a PIN.

## Authentication

Every call sends a RaceTime API token in the `X-RaceTime-Token` header. QR Timing needs a token that belongs to a
**RaceTime admin**, for two reasons:

* It acts on whichever event a QR code points to, without being a timekeeper there.
* The API returns athletes' PINs only to admins and to the event's managers.

The token is configured as `racetime.api.token` (environment variable `RACETIME_API_TOKEN`). Keep it in a secret store.

Base URL: `https://api.racetime.app`.

## 1. The event

```
GET /api/v1/events/{eventId}
```

```json
{ "id": "ev1", "title": "Spring Trail 10K", "timeZone": "Europe/Warsaw", "timingMethod": "INTEGRATION", "...": "..." }
```

QR Timing uses three fields:

| Field | Use |
|---|---|
| `title` | Shown above the check-in form. |
| `timeZone` | The recorded time is shown in it. If it is missing, UTC is used. |
| `timingMethod` | Who records the times. It must be `INTEGRATION` or `INTEGRATION_CHECKPOINTS`, otherwise the QR code counts as out of date. With `INTEGRATION_CHECKPOINTS`, timekeepers record the finish, so the finish takes no QR check-ins. RaceTime does not say which integration it is: every timing integration reads the same field. |

A `404` means the event does not exist, and the QR code counts as out of date.

## 2. The checkpoints

```
GET /api/v1/events/{eventId}/checkpoints
```

```json
[
  { "id": "s1", "type": "start", "name": "Start" },
  { "id": "c1", "type": "checkpoint", "name": "Windmill turn" },
  { "id": "c2", "type": "checkpoint", "name": null },
  { "id": "f1", "type": "finish", "name": "Finish", "isLap": false }
]
```

The list comes in course order. QR Timing looks up the QR code's `checkpointId` in it:

* If the ID is missing from the list, or it is the start, the QR code counts as out of date.
* A checkpoint without a name is shown by its position, for example "Checkpoint 2".

## 3. The athlete

```
GET /api/v1/events/{eventId}/participants?bib=214
```

```json
[
  { "id": "p1", "name": "Ann Runner", "bib": 214, "pin": "4821", "...": "..." },
  { "id": "p2", "name": "Ann Other", "bib": 214, "pin": null }
]
```

Bib numbers are not unique, so a search can return several athletes. QR Timing picks the athlete itself
(`ParticipantMatcher`):

* If a PIN was entered and exactly one athlete has it, that athlete is checked in.
* A wrong PIN is refused. It never falls back to an athlete without a PIN, so the wrong person can't be checked in by mistake.
* An athlete without a PIN can leave the PIN field empty or type anything.
* If the bib and PIN still match several athletes, the athlete is asked to see a race official.

Because QR Timing checks the PIN itself, it also limits guessing: after 5 wrong PINs for one bib, that bib is blocked
for 15 minutes.

## 4. The check-in

```
POST /api/v1/events/{eventId}/checkIn
```

```json
{ "checkpoint": "c1", "participant": "p1", "timestamp": "2026-05-17T12:32:08.412Z" }
```

`timestamp` is the moment the athlete's form reached QR Timing. The API checks that the check-in follows the course:

| Answer | Meaning | QR Timing shows |
|---|---|---|
| `200` | Recorded. | The confirmation. |
| `400` `{"startTime": "..."}` | The athlete has not started. | "Your start is not recorded". |
| `409` `{"code": "checkpoint_skipped"}` | An earlier checkpoint is missing. | "A checkpoint was missed". |
| `409` `{"code": "already_checked_in", "timestamp": "..."}` | The athlete already checked in here, or finished. | The confirmation again, with the earlier time. |

Any other answer, or no answer, means RaceTime is unavailable, and the athlete may try again.
