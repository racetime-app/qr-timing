package app.racetime.qrtiming.api;

/**
 * The RaceTime API could not be reached or gave an answer this app does not understand.
 *
 * <p>Examples: a timeout, a {@code 5xx} status, or a {@code 4xx} status without a known error code.
 * The check-in was not recorded, and the athlete is asked to try again.
 */
public final class RaceTimeUnavailableException extends RaceTimeApiException {

    public RaceTimeUnavailableException(String message) {
        super(message, null);
    }

    public RaceTimeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
