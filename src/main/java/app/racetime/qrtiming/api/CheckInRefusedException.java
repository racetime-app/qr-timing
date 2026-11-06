package app.racetime.qrtiming.api;

import java.time.Instant;

/**
 * {@code POST /api/v1/events/{eventId}/checkIn} was refused because the check-in does not fit the athlete's
 * progress along the course.
 */
public final class CheckInRefusedException extends RaceTimeApiException {

    /** Why the API refused the check-in. */
    public enum Reason {

        /** The athlete has no start time. The API answers 400 with the key {@code startTime}. */
        NOT_STARTED,

        /** The athlete missed an earlier checkpoint: 409 with {@code "code": "checkpoint_skipped"}. */
        CHECKPOINT_SKIPPED,

        /** The athlete already passed this checkpoint, or finished: 409 with {@code "code": "already_checked_in"}. */
        ALREADY_CHECKED_IN
    }

    private final Reason reason;
    private final Instant earlierCheckIn;

    public CheckInRefusedException(Reason reason, Instant earlierCheckIn) {
        super("RaceTime API refused the check-in: " + reason, null);
        this.reason = reason;
        this.earlierCheckIn = earlierCheckIn;
    }

    public Reason reason() {
        return reason;
    }

    /** For {@link Reason#ALREADY_CHECKED_IN}: when the earlier check-in was recorded, if the API knows. */
    public Instant earlierCheckIn() {
        return earlierCheckIn;
    }
}
