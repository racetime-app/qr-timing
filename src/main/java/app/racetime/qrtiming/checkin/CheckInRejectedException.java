package app.racetime.qrtiming.checkin;

/** A check-in that can't go ahead, for a reason the athlete is told about: {@link #error()}. */
public class CheckInRejectedException extends RuntimeException {

    private final CheckInError error;

    public CheckInRejectedException(CheckInError error) {
        super("Check-in rejected: " + error);
        this.error = error;
    }

    public CheckInError error() {
        return error;
    }
}
