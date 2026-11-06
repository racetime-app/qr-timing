package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The four RaceTime API calls QR Timing makes. They are the public RaceTime API, which any integration can use;
 * nothing here is specific to QR Timing.
 *
 * <p>Every call is authenticated with an API token (header {@code X-RaceTime-Token}). The token must belong to an
 * admin: it acts on whichever event a QR code points to, and it must be allowed to read the athletes' PINs.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link HttpRaceTimeApiClient} calls the real API.</li>
 *   <li>{@link DemoRaceTimeApiClient} answers from memory, for trying the app locally (profile {@code demo}).</li>
 * </ul>
 * Each method throws {@link RaceTimeUnavailableException} when the API can't be reached or answers unexpectedly.
 */
public interface RaceTimeApiClient {

    /**
     * {@code GET /api/v1/events/{eventId}}
     *
     * @return the event, or empty if there is no event with that ID
     */
    Optional<Event> findEvent(String eventId);

    /**
     * {@code GET /api/v1/events/{eventId}/checkpoints}
     *
     * @return the event's checkpoints in course order: start, checkpoints, finish
     */
    List<Checkpoint> getCheckpoints(String eventId);

    /**
     * {@code GET /api/v1/events/{eventId}/participants?bib={bib}}
     *
     * @return the athletes with that bib number: none, one, or several (bib numbers are not unique)
     */
    List<Participant> findParticipantsByBib(String eventId, int bib);

    /**
     * {@code POST /api/v1/events/{eventId}/checkIn}
     *
     * <p>Records that the athlete passed the checkpoint at the given time. The API checks the course order.
     *
     * @throws CheckInRefusedException if the athlete has not started, missed an earlier checkpoint, or already
     *                                 checked in here
     */
    void checkIn(String eventId, String checkpointId, String participantId, Instant timestamp);
}
