package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInError;

/** The RaceTime API refused the request, and {@link #error()} says why. */
public final class CheckInRejectedException extends RaceTimeApiException {

    private final CheckInError error;

    public CheckInRejectedException(CheckInError error) {
        super("RaceTime API rejected the request: " + error.code(), null);
        this.error = error;
    }

    /** Why the API refused the request. */
    public CheckInError error() {
        return error;
    }
}
