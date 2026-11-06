package app.racetime.qrtiming.api;

/**
 * A RaceTime API call that did not succeed.
 *
 * <p>There are exactly two kinds:
 * <ul>
 *   <li>{@link CheckInRefusedException}: the API answered a check-in and said no, for a reason about the athlete's
 *       progress (not started, skipped a checkpoint, already checked in).</li>
 *   <li>{@link RaceTimeUnavailableException}: there was no usable answer (network trouble, timeout, server error,
 *       a token the API did not accept). The athlete can only try again.</li>
 * </ul>
 * The class is {@code sealed}, so a {@code switch} over it is checked by the compiler to handle both.
 */
public abstract sealed class RaceTimeApiException extends RuntimeException
        permits CheckInRefusedException, RaceTimeUnavailableException {

    protected RaceTimeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
