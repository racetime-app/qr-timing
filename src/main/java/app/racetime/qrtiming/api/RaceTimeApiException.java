package app.racetime.qrtiming.api;

/**
 * A RaceTime API call that did not succeed.
 *
 * <p>There are exactly two kinds, and the pages treat them differently:
 * <ul>
 *   <li>{@link CheckInRejectedException}: the API answered and said no, for a reason the athlete can act on
 *       (wrong PIN, unknown bib number, ...).</li>
 *   <li>{@link RaceTimeUnavailableException}: there was no usable answer (network trouble, timeout, server error).
 *       The athlete can only try again.</li>
 * </ul>
 * The class is {@code sealed}, so a {@code switch} over it is checked by the compiler to handle both.
 */
public abstract sealed class RaceTimeApiException extends RuntimeException
        permits CheckInRejectedException, RaceTimeUnavailableException {

    protected RaceTimeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
