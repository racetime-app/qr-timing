package app.racetime.qrtiming.checkin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops PIN guessing: after {@link #MAX_FAILURES} wrong PINs for one bib number within {@link #WINDOW}, further
 * attempts for that bib are refused until the oldest failure is out of the window.
 *
 * <p>Because this app checks the PIN itself, it also has to limit the guessing itself. The counts live in memory,
 * so each running instance counts on its own and a restart clears them. That is enough to make guessing a 4-digit
 * PIN impractical; an app that needs more should keep the counts in a shared store.
 */
public class PinAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;

    /** Times of recent wrong PINs, keyed by "eventId/bib". */
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public PinAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Whether further attempts for this bib number are blocked for now. */
    public boolean isBlocked(String eventId, int bib) {
        Deque<Instant> recent = failures.get(key(eventId, bib));
        if (recent == null) {
            return false;
        }
        synchronized (recent) {
            forgetOld(recent);
            return recent.size() >= MAX_FAILURES;
        }
    }

    /** Counts a wrong PIN for this bib number. */
    public void recordFailure(String eventId, int bib) {
        Deque<Instant> recent = failures.computeIfAbsent(key(eventId, bib), k -> new ArrayDeque<>());
        synchronized (recent) {
            forgetOld(recent);
            recent.addLast(clock.instant());
        }
    }

    /** Clears the count after a successful check-in. */
    public void reset(String eventId, int bib) {
        failures.remove(key(eventId, bib));
    }

    private void forgetOld(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.removeFirst();
        }
    }

    private static String key(String eventId, int bib) {
        return eventId + "/" + bib;
    }
}
