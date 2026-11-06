package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An athlete registered for an event, as {@code GET /api/v1/events/{eventId}/participants} returns it. Only the
 * fields QR Timing uses.
 *
 * @param id   the participant ID, used to check them in
 * @param name the athlete's name
 * @param bib  the bib number. Not unique: different categories of one event may hand out the same numbers.
 * @param pin  the athlete's PIN, or {@code null} if they have none. The API includes it only for callers that may
 *             see it, such as an admin token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Participant(String id, String name, Integer bib, String pin) {

    /** Whether the athlete was given a PIN, and so must enter it to check in. */
    public boolean hasPin() {
        return pin != null && !pin.isBlank();
    }

    /** Leaves the PIN out, so a participant can be logged safely. */
    @Override
    public String toString() {
        return "Participant[id=" + id + ", bib=" + bib + "]";
    }
}
