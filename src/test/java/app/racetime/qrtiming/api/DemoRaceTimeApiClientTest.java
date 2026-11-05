package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInError;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.QrCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The demo API follows the same rules as the real one, so the pages can be tried in every state. */
class DemoRaceTimeApiClientTest {

    private static final QrCode FINISH = new QrCode("demo", "finish", "demo-key");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-05-17T12:00:00Z"));
    private final DemoRaceTimeApiClient demo = new DemoRaceTimeApiClient(clock);

    @Test
    void athleteWithPinChecksIn() {
        CheckInResult result = demo.checkIn(FINISH, "214", "4821");

        assertThat(result.bib()).isEqualTo("214");
        assertThat(result.checkpoint().isFinish()).isTrue();
    }

    @Test
    void athleteWithoutPinChecksInWhateverIsTyped() {
        assertThat(demo.checkIn(FINISH, "100", null).bib()).isEqualTo("100");
        assertThat(demo.checkIn(FINISH, "100", "9999").bib()).isEqualTo("100");
    }

    @Test
    void pinRules() {
        assertRejected(() -> demo.checkIn(FINISH, "214", null), CheckInError.PIN_REQUIRED);
        assertRejected(() -> demo.checkIn(FINISH, "214", "0000"), CheckInError.PIN_MISMATCH);
    }

    @Test
    void repeatedCheckInKeepsTheFirstTime() {
        Instant first = demo.checkIn(FINISH, "100", null).recordedAt();
        clock.advance(Duration.ofMinutes(5));

        assertThat(demo.checkIn(FINISH, "100", null).recordedAt()).isEqualTo(first);
    }

    @Test
    void failingBibs() {
        assertRejected(() -> demo.checkIn(FINISH, "300", null), CheckInError.CHECKPOINT_SKIPPED);
        assertRejected(() -> demo.checkIn(FINISH, "400", null), CheckInError.NOT_STARTED);
        assertRejected(() -> demo.checkIn(FINISH, "500", null), CheckInError.TOO_MANY_ATTEMPTS);
        assertRejected(() -> demo.checkIn(FINISH, "999", null), CheckInError.BIB_NOT_FOUND);
        assertThatThrownBy(() -> demo.checkIn(FINISH, "503", null)).isInstanceOf(RaceTimeUnavailableException.class);
    }

    @Test
    void unknownCodeIsOutdated() {
        assertRejected(() -> demo.findCheckpoint(new QrCode("demo", "finish", "old-key")), CheckInError.QR_OUTDATED);
        assertRejected(() -> demo.findCheckpoint(new QrCode("demo", "nowhere", "demo-key")), CheckInError.QR_OUTDATED);
    }

    private static void assertRejected(Runnable call, CheckInError expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(CheckInRejectedException.class, e -> assertThat(e.error()).isEqualTo(expected));
    }

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
