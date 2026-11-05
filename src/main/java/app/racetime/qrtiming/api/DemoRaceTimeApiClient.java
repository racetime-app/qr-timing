package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInError;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.CheckpointInfo;
import app.racetime.qrtiming.api.model.CheckpointType;
import app.racetime.qrtiming.api.model.QrCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in for the RaceTime API that answers from memory. Active with the {@code demo} profile.
 *
 * <p>It lets anyone run the app and see every page without a RaceTime account or event. It follows the same
 * rules as the real API (see {@code docs/api-contract.md}), including returning the first recorded time when
 * an athlete checks in twice.
 *
 * <p>Demo QR codes (open {@code http://localhost:8080} plus the path):
 * <ul>
 *   <li>{@code /c/demo/windmill/demo-key}: a checkpoint with a name</li>
 *   <li>{@code /c/demo/cp3/demo-key}: a checkpoint without a name, shown by its number</li>
 *   <li>{@code /c/demo/finish/demo-key}: the finish</li>
 *   <li>any other key, such as {@code /c/demo/finish/old-key}: an out-of-date code</li>
 * </ul>
 *
 * <p>Demo bib numbers:
 * <ul>
 *   <li>{@code 214}, PIN {@code 4821}: checks in. Leave the PIN empty or enter another one to see the PIN errors.</li>
 *   <li>{@code 100}, no PIN: checks in, whatever is typed into the PIN field.</li>
 *   <li>{@code 300}: missed an earlier checkpoint.</li>
 *   <li>{@code 400}: has not started.</li>
 *   <li>{@code 500}: blocked after too many wrong PINs.</li>
 *   <li>{@code 503}: the API is "down".</li>
 *   <li>anything else: bib number not found.</li>
 * </ul>
 */
public class DemoRaceTimeApiClient implements RaceTimeApiClient {

    private static final Logger log = LoggerFactory.getLogger(DemoRaceTimeApiClient.class);

    static final String EVENT_ID = "demo";
    static final String KEY = "demo-key";

    private static final String EVENT_NAME = "Spring Trail 10K";

    /** The demo checkpoints, by checkpoint id. */
    private static final Map<String, Checkpoint> CHECKPOINTS = Map.of(
            "windmill", new Checkpoint("Windmill turn", CheckpointType.CHECKPOINT, 2),
            "cp3", new Checkpoint(null, CheckpointType.CHECKPOINT, 3),
            "finish", new Checkpoint("Finish", CheckpointType.FINISH, null));

    /** Athletes who check in successfully, with their PIN ({@code ""} means no PIN). */
    private static final Map<String, String> PINS = Map.of(
            "214", "4821",
            "100", "");

    /** Bib numbers that always fail the same way. */
    private static final Map<String, CheckInError> FAILING_BIBS = Map.of(
            "300", CheckInError.CHECKPOINT_SKIPPED,
            "400", CheckInError.NOT_STARTED,
            "500", CheckInError.TOO_MANY_ATTEMPTS);

    /** Bib number that simulates the API being unreachable. */
    private static final String UNAVAILABLE_BIB = "503";

    /** Recorded check-ins, keyed by "checkpointId/bib", so a repeated check-in returns the first time. */
    private final Map<String, Instant> checkIns = new ConcurrentHashMap<>();

    private final Clock clock;

    public DemoRaceTimeApiClient() {
        this(Clock.systemUTC());
    }

    /** @param clock source of the "recorded" time; tests pass a fixed clock */
    public DemoRaceTimeApiClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CheckpointInfo findCheckpoint(QrCode qrCode) {
        return new CheckpointInfo(EVENT_NAME, checkpointOf(qrCode));
    }

    @Override
    public CheckInResult checkIn(QrCode qrCode, String bib, String pin) {
        Checkpoint checkpoint = checkpointOf(qrCode);

        if (UNAVAILABLE_BIB.equals(bib)) {
            throw new RaceTimeUnavailableException("Demo: RaceTime API is unavailable");
        }
        if (FAILING_BIBS.containsKey(bib)) {
            throw new CheckInRejectedException(FAILING_BIBS.get(bib));
        }
        if (!PINS.containsKey(bib)) {
            throw new CheckInRejectedException(CheckInError.BIB_NOT_FOUND);
        }

        // The PIN rules of the real API: only athletes who have a PIN must enter it.
        String expectedPin = PINS.get(bib);
        if (!expectedPin.isEmpty()) {
            if (pin == null || pin.isEmpty()) {
                throw new CheckInRejectedException(CheckInError.PIN_REQUIRED);
            }
            if (!expectedPin.equals(pin)) {
                throw new CheckInRejectedException(CheckInError.PIN_MISMATCH);
            }
        }

        Instant recordedAt = checkIns.computeIfAbsent(qrCode.checkpointId() + "/" + bib, id -> clock.instant());
        return new CheckInResult(EVENT_NAME, checkpoint, bib, recordedAt, ZoneId.systemDefault().getId());
    }

    /** The demo checkpoint of a QR code, or {@code QR_OUTDATED} if the code isn't one of the demo codes. */
    private static Checkpoint checkpointOf(QrCode qrCode) {
        Checkpoint checkpoint = CHECKPOINTS.get(qrCode.checkpointId());
        if (!EVENT_ID.equals(qrCode.eventId()) || !KEY.equals(qrCode.key()) || checkpoint == null) {
            throw new CheckInRejectedException(CheckInError.QR_OUTDATED);
        }
        return checkpoint;
    }

    /** Prints the demo links once the server is up, so they can be opened straight from the console. */
    @EventListener(ApplicationReadyEvent.class)
    public void printDemoLinks(ApplicationReadyEvent event) {
        String port = Objects.requireNonNullElse(
                event.getApplicationContext().getEnvironment().getProperty("local.server.port"), "8080");
        String base = "http://localhost:" + port;
        log.info("""
                QR Timing is running with the demo API. Try these QR codes:
                  Named checkpoint:    {}/c/demo/windmill/demo-key
                  Numbered checkpoint: {}/c/demo/cp3/demo-key
                  Finish:              {}/c/demo/finish/demo-key
                  Out-of-date code:    {}/c/demo/finish/old-key
                Bibs: 214 (PIN 4821), 100 (no PIN), 300 skipped, 400 not started, 500 locked, 503 API down,\
                 anything else not found.""", base, base, base, base);
    }
}
