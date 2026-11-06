package app.racetime.qrtiming.checkin;

/**
 * The reasons a check-in can't go ahead. Each one has its own page that tells the athlete what went wrong and
 * what to do next (see {@code web.ErrorScreen}).
 */
public enum CheckInError {

    /**
     * The QR code leads nowhere: the event or checkpoint no longer exists, the event's timing is not left to an
     * integration, or this is the finish of an event where timekeepers record the finish.
     */
    QR_OUTDATED,

    /** No athlete in this event has that bib number. */
    BIB_NOT_FOUND,

    /** Several athletes have that bib number, and the PIN (if any) doesn't tell them apart. */
    BIB_AMBIGUOUS,

    /** The athlete has a PIN, but none was entered. */
    PIN_REQUIRED,

    /** The athlete has a PIN, and the one entered is wrong. */
    PIN_MISMATCH,

    /** Too many wrong PINs were entered for this bib number recently. */
    TOO_MANY_ATTEMPTS,

    /** The athlete has no start time yet. */
    NOT_STARTED,

    /** The athlete has no check-in at one or more earlier checkpoints. */
    CHECKPOINT_SKIPPED
}
