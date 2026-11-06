package app.racetime.qrtiming.checkin;

import app.racetime.qrtiming.api.CheckInRefusedException;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Checks athletes in, using nothing but the public RaceTime API.
 *
 * <p>One check-in takes four API calls:
 * <ol>
 *   <li>{@code GET /api/v1/events/{eventId}}: the event exists and its timing is left to an integration;</li>
 *   <li>{@code GET /api/v1/events/{eventId}/checkpoints}: the QR code's checkpoint exists, and where it is on the
 *       course;</li>
 *   <li>{@code GET /api/v1/events/{eventId}/participants?bib=...}: who has the bib number, and their PINs;</li>
 *   <li>{@code POST /api/v1/events/{eventId}/checkIn}: record the time. The API checks the course order.</li>
 * </ol>
 * Opening the form takes the first two only.
 *
 * <p>Errors the athlete can act on are thrown as {@link CheckInRejectedException}. When the API can't be reached,
 * {@link app.racetime.qrtiming.api.RaceTimeUnavailableException} passes through.
 */
public class CheckInService {

    private final RaceTimeApiClient raceTime;
    private final PinAttemptLimiter pinAttempts;
    private final Clock clock;

    public CheckInService(RaceTimeApiClient raceTime, PinAttemptLimiter pinAttempts, Clock clock) {
        this.raceTime = raceTime;
        this.pinAttempts = pinAttempts;
        this.clock = clock;
    }

    /**
     * Where a QR code leads: the event and checkpoint to show above the form.
     *
     * @throws CheckInRejectedException with {@code QR_OUTDATED} if the code can't be used for checking in
     */
    public CheckpointInfo findCheckpoint(QrCode qrCode) {
        return resolve(qrCode).info();
    }

    /**
     * Checks an athlete in at the checkpoint of a QR code.
     *
     * @param qrCode the scanned code
     * @param bib    the bib number the athlete entered
     * @param pin    the PIN the athlete entered, or {@code null} if they left the field empty
     * @return the recorded check-in, or the earlier one if the athlete had already checked in here
     */
    public CheckInResult checkIn(QrCode qrCode, int bib, String pin) {
        // The athlete's time is the moment their request arrived, not when the API calls are done.
        Instant now = clock.instant();

        ResolvedCheckpoint place = resolve(qrCode);
        Participant athlete = identify(qrCode.eventId(), bib, pin);

        try {
            raceTime.checkIn(qrCode.eventId(), qrCode.checkpointId(), athlete.id(), now);
            return place.result(bib, now, false);
        } catch (CheckInRefusedException refused) {
            return switch (refused.reason()) {
                case NOT_STARTED -> throw new CheckInRejectedException(CheckInError.NOT_STARTED);
                case CHECKPOINT_SKIPPED -> throw new CheckInRejectedException(CheckInError.CHECKPOINT_SKIPPED);
                // Checking in twice is harmless: show the first time again.
                case ALREADY_CHECKED_IN -> place.result(bib, refused.earlierCheckIn(), true);
            };
        }
    }

    /** Finds the athlete by bib number and PIN, counting wrong PINs to stop guessing. */
    private Participant identify(String eventId, int bib, String pin) {
        if (pinAttempts.isBlocked(eventId, bib)) {
            throw new CheckInRejectedException(CheckInError.TOO_MANY_ATTEMPTS);
        }
        List<Participant> withBib = raceTime.findParticipantsByBib(eventId, bib);
        try {
            Participant athlete = ParticipantMatcher.match(withBib, pin);
            pinAttempts.reset(eventId, bib);
            return athlete;
        } catch (CheckInRejectedException e) {
            if (e.error() == CheckInError.PIN_MISMATCH) {
                pinAttempts.recordFailure(eventId, bib);
            }
            throw e;
        }
    }

    /** Looks up the event and the checkpoint of a QR code, and checks the code may be used. */
    private ResolvedCheckpoint resolve(QrCode qrCode) {
        Event event = raceTime.findEvent(qrCode.eventId())
                .filter(Event::timedByIntegration)
                .orElseThrow(() -> new CheckInRejectedException(CheckInError.QR_OUTDATED));

        List<Checkpoint> course = raceTime.getCheckpoints(qrCode.eventId());
        Checkpoint checkpoint = course.stream()
                .filter(candidate -> candidate.id().equals(qrCode.checkpointId()))
                .findFirst()
                .orElseThrow(() -> new CheckInRejectedException(CheckInError.QR_OUTDATED));

        // Nothing is recorded at the start; and with INTEGRATION_CHECKPOINTS, timekeepers record the finish.
        if (checkpoint.isStart() || (checkpoint.isFinish() && !event.finishTimedByIntegration())) {
            throw new CheckInRejectedException(CheckInError.QR_OUTDATED);
        }

        return new ResolvedCheckpoint(event, new CheckpointView(checkpoint.name(), checkpoint.isFinish(),
                numberOf(checkpoint, course)));
    }

    /** Position of a checkpoint among the course's intermediate checkpoints, from 1; null for the finish. */
    private static Integer numberOf(Checkpoint checkpoint, List<Checkpoint> course) {
        if (!checkpoint.isIntermediate()) {
            return null;
        }
        List<Checkpoint> intermediate = course.stream().filter(Checkpoint::isIntermediate).toList();
        return intermediate.indexOf(checkpoint) + 1;
    }

    /** An event and one of its checkpoints, looked up and checked. */
    private record ResolvedCheckpoint(Event event, CheckpointView checkpoint) {

        CheckpointInfo info() {
            return new CheckpointInfo(event.title(), checkpoint);
        }

        CheckInResult result(int bib, Instant recordedAt, boolean repeated) {
            return new CheckInResult(event.title(), checkpoint, String.valueOf(bib), recordedAt, event.timeZone(),
                    repeated);
        }
    }
}
