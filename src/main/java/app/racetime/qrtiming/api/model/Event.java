package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A RaceTime event, as {@code GET /api/v1/events/{eventId}} returns it. Only the fields QR Timing uses.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} keeps the app working when the API adds fields, which is
 * a good habit for any API client.
 *
 * @param id           the event ID
 * @param title        the name of the event, shown to the athlete
 * @param timeZone     IANA time-zone id of the event, such as "Europe/Warsaw"; may be {@code null}
 * @param timingMethod who records the times, one of the {@code TIMING_*} values; {@code null} means manual
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Event(String id, String title, String timeZone, String timingMethod) {

    /** Timekeepers in the RaceTime app record every time. */
    public static final String TIMING_MANUAL = "MANUAL";

    /** A timing integration, such as QR Timing, records the times at the checkpoints and at the finish. */
    public static final String TIMING_INTEGRATION = "INTEGRATION";

    /** A timing integration records the checkpoints; timekeepers record the finish. */
    public static final String TIMING_INTEGRATION_CHECKPOINTS = "INTEGRATION_CHECKPOINTS";

    /**
     * Whether the organiser leaves the checkpoints to a timing integration. RaceTime doesn't say which one:
     * whoever deploys QR Timing for an event chooses it by putting its QR codes up.
     */
    public boolean timedByIntegration() {
        return TIMING_INTEGRATION.equals(timingMethod) || TIMING_INTEGRATION_CHECKPOINTS.equals(timingMethod);
    }

    /** Whether the integration records the finish too. With INTEGRATION_CHECKPOINTS, timekeepers record it. */
    public boolean finishTimedByIntegration() {
        return TIMING_INTEGRATION.equals(timingMethod);
    }
}
