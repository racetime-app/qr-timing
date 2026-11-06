package app.racetime.qrtiming.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The demo API follows the course-order rules of the real one, so every page can be tried. */
class DemoRaceTimeApiClientTest {

    private static final Instant T1 = Instant.parse("2026-05-17T12:00:00Z");
    private static final Instant T2 = Instant.parse("2026-05-17T12:30:00Z");

    private final DemoRaceTimeApiClient demo = new DemoRaceTimeApiClient();

    @Test
    void checkpointsInCourseOrder() {
        demo.checkIn("demo", "windmill", "p-214", T1);
        demo.checkIn("demo", "cp2", "p-214", T2);
        demo.checkIn("demo", "finish", "p-214", T2);
    }

    @Test
    void aMissedCheckpoint() {
        assertRefused(() -> demo.checkIn("demo", "cp2", "p-100", T1), CheckInRefusedException.Reason.CHECKPOINT_SKIPPED);
    }

    @Test
    void aRepeatedCheckInReturnsTheFirstTime() {
        demo.checkIn("demo", "windmill", "p-100", T1);

        assertThatThrownBy(() -> demo.checkIn("demo", "windmill", "p-100", T2))
                .isInstanceOfSatisfying(CheckInRefusedException.class,
                        e -> assertThat(e.earlierCheckIn()).isEqualTo(T1));
    }

    @Test
    void notStarted() {
        assertRefused(() -> demo.checkIn("demo", "windmill", "p-400", T1), CheckInRefusedException.Reason.NOT_STARTED);
    }

    @Test
    void searchByBib() {
        assertThat(demo.findParticipantsByBib("demo", 777)).hasSize(2);
        assertThat(demo.findParticipantsByBib("demo", 999)).isEmpty();
        assertThatThrownBy(() -> demo.findParticipantsByBib("demo", 503))
                .isInstanceOf(RaceTimeUnavailableException.class);
    }

    private static void assertRefused(Runnable call, CheckInRefusedException.Reason reason) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(CheckInRefusedException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    }
}
