# QR Timing API contract

This is the part of the RaceTime API that QR Timing uses. It is small on purpose: two calls, both made by the
server-side app, never by the athlete's browser.

> **Status:** draft. The endpoints below are what this app is written against. Until they are available in
> the RaceTime API, run the app with the `demo` profile (see [README](../README.md)).

## The QR code

When an organiser turns on QR timing for an event, RaceTime gives every checkpoint (and the finish) its own QR
code. The code holds a URL of this app:

```
https://<qr-timing-host>/c/{eventId}/{checkpointId}/{key}
```

| Part | Meaning |
|---|---|
| `eventId` | The RaceTime event. |
| `checkpointId` | The checkpoint, or the finish, inside that event. |
| `key` | A random secret stored with the checkpoint. It proves the athlete scanned the real code. |

The organiser can create a new key at any time, for example when a code has been photographed and shared. Old
printed codes then stop working, and athletes who scan them see "This QR code is out of date".

## Authentication

None. The `key` in the path is what allows the call, so an integration needs no RaceTime credentials. This keeps
the app easy to run and easy to copy. Anyone holding a valid code can do exactly what an athlete standing at the
checkpoint can do, and nothing more.

## Common rules

* Base URL: `https://api.racetime.app`.
* Requests and responses are JSON (`Content-Type: application/json`).
* Unknown fields in responses must be ignored, so new fields can be added without breaking clients.
* Errors that the athlete can act on come back as a `4xx` status with a body holding a machine-readable `code`:

  ```json
  { "code": "bib_not_found" }
  ```

  Any other failure, such as a `5xx`, a `4xx` without a known `code`, or a timeout, means the check-in could not
  be completed and the athlete may try again.

## 1. Look up a checkpoint

```
GET /api/v1/qr/{eventId}/{checkpointId}/{key}
```

Called when an athlete opens the link from a QR code, to show which event and checkpoint they are at.

**200 OK**

```json
{
  "eventName": "Spring Trail 10K",
  "checkpoint": {
    "name": "Windmill turn",
    "type": "CHECKPOINT",
    "number": 3
  }
}
```

| Field | Type | Meaning |
|---|---|---|
| `eventName` | string | Name of the event. |
| `checkpoint.name` | string, optional | Name the organiser gave the checkpoint. May be missing or empty. |
| `checkpoint.type` | `CHECKPOINT` or `FINISH` | Whether this is an intermediate checkpoint or the finish line. |
| `checkpoint.number` | integer, optional | Position of the checkpoint along the course, starting at 1. Shown when it has no name. |

**Errors:** `qr_outdated` (see the table below).

## 2. Check in

```
POST /api/v1/qr/{eventId}/{checkpointId}/{key}/checkIn
```

```json
{ "bib": "214", "pin": "4821" }
```

| Field | Type | Meaning |
|---|---|---|
| `bib` | string of digits | The athlete's bib number. Required. |
| `pin` | string of digits, optional | The athlete's PIN. Leave it out, or send `null`, if the athlete did not enter one. |

PINs are optional for every athlete:

* If the athlete has a PIN, it must be sent and it must match.
* If the athlete has no PIN, the `pin` field is ignored.

The API records the time at the moment it receives the request. Clients cannot send a time of their own.

Checking in again at the same checkpoint does not create a second record: the API returns the time recorded the
first time, with `200 OK`. That makes a retry, or a page reload, harmless.

**200 OK**

```json
{
  "eventName": "Spring Trail 10K",
  "checkpoint": { "name": "Windmill turn", "type": "CHECKPOINT", "number": 3 },
  "bib": "214",
  "recordedAt": "2026-05-17T12:32:08.412Z",
  "timeZone": "Europe/Warsaw"
}
```

| Field | Type | Meaning |
|---|---|---|
| `eventName`, `checkpoint` | | As in the lookup. |
| `bib` | string | The bib number that was checked in. |
| `recordedAt` | ISO-8601 instant | When the check-in was recorded. |
| `timeZone` | IANA time-zone id, optional | Time zone of the event, for showing `recordedAt` as a local time. UTC if missing. |

**Errors:** any code in the table below.

## Error codes

| Code | HTTP status | When |
|---|---|---|
| `qr_outdated` | 404 | The key is wrong or was replaced, the checkpoint was deleted, or QR timing is off for the event. |
| `bib_not_found` | 422 | No athlete in this event has that bib number. |
| `pin_required` | 422 | The athlete has a PIN, but none was sent. |
| `pin_mismatch` | 422 | The athlete has a PIN, and the one sent is wrong. |
| `checkpoint_skipped` | 422 | The athlete has no check-in at one or more earlier checkpoints. |
| `not_started` | 422 | The athlete has no start time yet. |
| `too_many_attempts` | 429 | Too many wrong PINs were sent for this athlete. The API blocks further attempts for a while. |

New codes may be added later. A client that receives a code it does not know should treat it like any other
failure.
