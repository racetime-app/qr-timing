package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in for the RaceTime API that answers from memory. Active with the {@code demo} profile.
 *
 * <p>It lets anyone run the app and see every page without a RaceTime account. It follows the rules of the real
 * API: checkpoints must be passed in course order, and a repeated check-in is refused with the earlier time.
 *
 * <p>Demo events and QR codes (open {@code http://localhost:8080} plus the path):
 * <ul>
 *   <li>{@code /c/demo/windmill}: checkpoint 1, named "Windmill turn"</li>
 *   <li>{@code /c/demo/cp2}: checkpoint 2, without a name</li>
 *   <li>{@code /c/demo/finish}: the finish</li>
 *   <li>{@code /c/manual/finish}: an event timed by timekeepers only, so its QR code is "out of date"</li>
 * </ul>
 *
 * <p>Demo athletes (bib numbers):
 * <ul>
 *   <li>{@code 214}, PIN {@code 4821}: has started. Leave out the PIN or enter another one to see the PIN errors;
 *       five wrong PINs in a row block the bib for a while.</li>
 *   <li>{@code 100}, no PIN: has started.</li>
 *   <li>{@code 777}: two athletes share this bib, with PINs {@code 1111} and {@code 2222}.</li>
 *   <li>{@code 400}: has not started.</li>
 *   <li>{@code 503}: the API is "down".</li>
 * </ul>
 * Scanning {@code cp2} or the finish before {@code windmill} shows the "checkpoint missed" page, and scanning a
 * checkpoint twice shows the confirmation again.
 */
public class DemoRaceTimeApiClient implements RaceTimeApiClient {

    private static final Logger log = LoggerFactory.getLogger(DemoRaceTimeApiClient.class);

    private static final String TIME_ZONE = ZoneId.systemDefault().getId();

    private static final Map<String, Event> EVENTS = Map.of(
            "demo", new Event("demo", "Spring Trail 10K", TIME_ZONE, Event.TIMING_INTEGRATION),
            "manual", new Event("manual", "Autumn Road 5K", TIME_ZONE, Event.TIMING_MANUAL));

    /** The same course for both events, in course order. */
    private static final List<Checkpoint> COURSE = List.of(
            new Checkpoint("start", Checkpoint.TYPE_START, "Start", false),
            new Checkpoint("windmill", Checkpoint.TYPE_CHECKPOINT, "Windmill turn", false),
            new Checkpoint("cp2", Checkpoint.TYPE_CHECKPOINT, null, false),
            new Checkpoint("finish", Checkpoint.TYPE_FINISH, "Finish", false));

    private static final List<Participant> PARTICIPANTS = List.of(
            new Participant("p-214", "Ann Runner", 214, "4821"),
            new Participant("p-100", "Ben Walker", 100, null),
            new Participant("p-777a", "Cleo Hill", 777, "1111"),
            new Participant("p-777b", "Dan Hill", 777, "2222"),
            new Participant("p-400", "Eve Late", 400, null));

    private static final Set<String> NOT_STARTED = Set.of("p-400");

    /** Bib number that simulates the API being unreachable. */
    private static final int UNAVAILABLE_BIB = 503;

    /** How many checkpoints after the start each athlete has passed (0 = none yet). */
    private final Map<String, Integer> passed = new ConcurrentHashMap<>();

    /** Recorded check-ins, keyed by "participantId/checkpointId". */
    private final Map<String, Instant> checkIns = new ConcurrentHashMap<>();

    @Override
    public Optional<Event> findEvent(String eventId) {
        return Optional.ofNullable(EVENTS.get(eventId));
    }

    @Override
    public List<Checkpoint> getCheckpoints(String eventId) {
        return EVENTS.containsKey(eventId) ? COURSE : List.of();
    }

    @Override
    public List<Participant> findParticipantsByBib(String eventId, int bib) {
        if (bib == UNAVAILABLE_BIB) {
            throw new RaceTimeUnavailableException("Demo: RaceTime API is unavailable");
        }
        return PARTICIPANTS.stream().filter(participant -> participant.bib() == bib).toList();
    }

    @Override
    public synchronized void checkIn(String eventId, String checkpointId, String participantId, Instant timestamp) {
        if (NOT_STARTED.contains(participantId)) {
            throw new CheckInRefusedException(CheckInRefusedException.Reason.NOT_STARTED, null);
        }

        // Positions after the start: windmill = 1, cp2 = 2, finish = 3.
        int position = COURSE.stream().map(Checkpoint::id).toList().indexOf(checkpointId);
        int passedSoFar = passed.getOrDefault(participantId, 0);

        if (position <= passedSoFar) {
            throw new CheckInRefusedException(CheckInRefusedException.Reason.ALREADY_CHECKED_IN,
                    checkIns.get(participantId + "/" + checkpointId));
        }
        if (position > passedSoFar + 1) {
            throw new CheckInRefusedException(CheckInRefusedException.Reason.CHECKPOINT_SKIPPED, null);
        }
        passed.put(participantId, position);
        checkIns.put(participantId + "/" + checkpointId, timestamp);
    }

    /** Prints the demo links once the server is up, so they can be opened straight from the console. */
    @EventListener(ApplicationReadyEvent.class)
    public void printDemoLinks(ApplicationReadyEvent event) {
        String port = Objects.requireNonNullElse(
                event.getApplicationContext().getEnvironment().getProperty("local.server.port"), "8080");
        String base = "http://localhost:" + port;
        log.info("""
                QR Timing is running with the demo API. Try these QR codes:
                  Checkpoint 1:     {}/c/demo/windmill
                  Checkpoint 2:     {}/c/demo/cp2
                  Finish:           {}/c/demo/finish
                  Out-of-date code: {}/c/manual/finish
                Bibs: 214 (PIN 4821), 100 (no PIN), 777 (two athletes, PINs 1111 and 2222), 400 not started,\
                 503 API down, anything else not found.""", base, base, base, base);
    }
}
