package app.racetime.qrtiming.checkin;

import app.racetime.qrtiming.api.CheckInRefusedException;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The check-in flow on top of a mocked RaceTime API. */
class CheckInServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-17T12:32:08Z");
    private static final String EVENT_ID = "event-1";

    private static final List<Checkpoint> COURSE = List.of(
            new Checkpoint("start", "start", "Start", false),
            new Checkpoint("windmill", "checkpoint", "Windmill turn", false),
            new Checkpoint("cp2", "checkpoint", null, false),
            new Checkpoint("finish", "finish", "Finish", false));

    private static final Participant ANN = new Participant("p1", "Ann Runner", 214, "4821");

    private final RaceTimeApiClient api = mock(RaceTimeApiClient.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PinAttemptLimiter limiter = new PinAttemptLimiter(clock);
    private final CheckInService service = new CheckInService(api, limiter, clock);

    @BeforeEach
    void setUp() {
        givenEvent(Event.TIMING_INTEGRATION);
        when(api.getCheckpoints(EVENT_ID)).thenReturn(COURSE);
        when(api.findParticipantsByBib(EVENT_ID, 214)).thenReturn(List.of(ANN));
    }

    @Test
    void namedCheckpoint() {
        CheckpointInfo info = service.findCheckpoint(new QrCode(EVENT_ID, "windmill"));

        assertThat(info.eventName()).isEqualTo("Spring Trail 10K");
        assertThat(info.checkpoint()).isEqualTo(new CheckpointView("Windmill turn", false, 1));
    }

    @Test
    void checkpointsAreNumberedAlongTheCourse() {
        assertThat(service.findCheckpoint(new QrCode(EVENT_ID, "cp2")).checkpoint().number()).isEqualTo(2);
        assertThat(service.findCheckpoint(new QrCode(EVENT_ID, "finish")).checkpoint())
                .isEqualTo(new CheckpointView("Finish", true, null));
    }

    @Test
    void qrCodesThatCantBeUsed() {
        assertOutdated(new QrCode(EVENT_ID, "deleted"));
        assertOutdated(new QrCode(EVENT_ID, "start"));
        assertOutdated(new QrCode("no-such-event", "finish"));
    }

    @Test
    void eventsTimedByTimekeepers() {
        givenEvent(null);
        assertOutdated(new QrCode(EVENT_ID, "windmill"));

        givenEvent(Event.TIMING_MANUAL);
        assertOutdated(new QrCode(EVENT_ID, "windmill"));
    }

    @Test
    void integrationAtCheckpointsLeavesTheFinishToTimekeepers() {
        givenEvent(Event.TIMING_INTEGRATION_CHECKPOINTS);

        assertThat(service.findCheckpoint(new QrCode(EVENT_ID, "windmill")).checkpoint().name())
                .isEqualTo("Windmill turn");
        assertOutdated(new QrCode(EVENT_ID, "finish"));
    }

    @Test
    void checkInRecordsTheTimeTheRequestArrived() {
        CheckInResult result = service.checkIn(new QrCode(EVENT_ID, "windmill"), 214, "4821");

        verify(api).checkIn(EVENT_ID, "windmill", "p1", NOW);
        assertThat(result.recordedAt()).isEqualTo(NOW);
        assertThat(result.repeated()).isFalse();
        assertThat(result.localRecordedAt()).hasValueSatisfying(
                time -> assertThat(time.toLocalTime()).hasToString("14:32:08"));
    }

    @Test
    void aRepeatedCheckInShowsTheEarlierTime() {
        Instant earlier = Instant.parse("2026-05-17T12:10:00Z");
        doThrow(new CheckInRefusedException(CheckInRefusedException.Reason.ALREADY_CHECKED_IN, earlier))
                .when(api).checkIn(EVENT_ID, "windmill", "p1", NOW);

        CheckInResult result = service.checkIn(new QrCode(EVENT_ID, "windmill"), 214, "4821");

        assertThat(result.repeated()).isTrue();
        assertThat(result.recordedAt()).isEqualTo(earlier);
    }

    @Test
    void refusalsFromTheApi() {
        doThrow(new CheckInRefusedException(CheckInRefusedException.Reason.CHECKPOINT_SKIPPED, null))
                .when(api).checkIn(EVENT_ID, "cp2", "p1", NOW);
        doThrow(new CheckInRefusedException(CheckInRefusedException.Reason.NOT_STARTED, null))
                .when(api).checkIn(EVENT_ID, "finish", "p1", NOW);

        assertRejected(() -> service.checkIn(new QrCode(EVENT_ID, "cp2"), 214, "4821"),
                CheckInError.CHECKPOINT_SKIPPED);
        assertRejected(() -> service.checkIn(new QrCode(EVENT_ID, "finish"), 214, "4821"),
                CheckInError.NOT_STARTED);
    }

    @Test
    void wrongPinsAreLimited() {
        QrCode windmill = new QrCode(EVENT_ID, "windmill");
        for (int attempt = 0; attempt < PinAttemptLimiter.MAX_FAILURES; attempt++) {
            assertRejected(() -> service.checkIn(windmill, 214, "0000"), CheckInError.PIN_MISMATCH);
        }

        // Even the right PIN is refused now, and RaceTime isn't asked any more.
        assertRejected(() -> service.checkIn(windmill, 214, "4821"), CheckInError.TOO_MANY_ATTEMPTS);
        verify(api, never()).checkIn(anyString(), anyString(), anyString(), any());
    }

    @Test
    void anOutdatedCodeIsReportedBeforeLookingForTheAthlete() {
        assertOutdated(new QrCode(EVENT_ID, "deleted"));

        assertRejected(() -> service.checkIn(new QrCode(EVENT_ID, "deleted"), 214, "4821"),
                CheckInError.QR_OUTDATED);
        verify(api, never()).findParticipantsByBib(anyString(), anyInt());
    }

    private void givenEvent(String timingMethod) {
        when(api.findEvent(EVENT_ID)).thenReturn(
                Optional.of(new Event(EVENT_ID, "Spring Trail 10K", "Europe/Warsaw", timingMethod)));
    }

    private void assertOutdated(QrCode qrCode) {
        assertRejected(() -> service.findCheckpoint(qrCode), CheckInError.QR_OUTDATED);
    }

    private static void assertRejected(Runnable call, CheckInError expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(CheckInRejectedException.class, e -> assertThat(e.error()).isEqualTo(expected));
    }
}
