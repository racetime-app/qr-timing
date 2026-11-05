package app.racetime.qrtiming.api.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * The reasons the RaceTime API gives for not accepting a check-in.
 *
 * <p>The API sends them as a {@code 4xx} response with a body like {@code {"code": "bib_not_found"}}. Each one has
 * its own page that tells the athlete what went wrong and what to do next.
 */
public enum CheckInError {

    /** The key in the QR code was replaced, the checkpoint was deleted, or QR timing is off for the event. */
    QR_OUTDATED("qr_outdated"),

    /** No athlete in this event has that bib number. */
    BIB_NOT_FOUND("bib_not_found"),

    /** The athlete has a PIN, but none was entered. */
    PIN_REQUIRED("pin_required"),

    /** The athlete has a PIN, and the one entered is wrong. */
    PIN_MISMATCH("pin_mismatch"),

    /** The athlete has no check-in at one or more earlier checkpoints. */
    CHECKPOINT_SKIPPED("checkpoint_skipped"),

    /** The athlete has no start time yet. */
    NOT_STARTED("not_started"),

    /** Too many wrong PINs were entered for this athlete; the API blocks further attempts for a while. */
    TOO_MANY_ATTEMPTS("too_many_attempts");

    private final String code;

    CheckInError(String code) {
        this.code = code;
    }

    /** The value of the {@code code} field in the API's error response. */
    public String code() {
        return code;
    }

    /**
     * Finds the error for a {@code code} from an API response.
     *
     * @return the matching error, or empty for a code this version of the app does not know yet
     */
    public static Optional<CheckInError> fromCode(String code) {
        return Arrays.stream(values())
                .filter(error -> error.code.equals(code))
                .findFirst();
    }
}
