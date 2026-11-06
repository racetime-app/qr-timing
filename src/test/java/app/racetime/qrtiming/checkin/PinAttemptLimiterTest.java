package app.racetime.qrtiming.checkin;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PinAttemptLimiterTest {

    private final MovableClock clock = new MovableClock(Instant.parse("2026-05-17T12:00:00Z"));
    private final PinAttemptLimiter limiter = new PinAttemptLimiter(clock);

    @Test
    void blocksAfterTooManyFailures() {
        failTimes(PinAttemptLimiter.MAX_FAILURES - 1);
        assertThat(limiter.isBlocked("e", 214)).isFalse();

        failTimes(1);
        assertThat(limiter.isBlocked("e", 214)).isTrue();
        assertThat(limiter.isBlocked("e", 215)).isFalse();
        assertThat(limiter.isBlocked("other-event", 214)).isFalse();
    }

    @Test
    void failuresExpire() {
        failTimes(PinAttemptLimiter.MAX_FAILURES);
        clock.advance(PinAttemptLimiter.WINDOW.plusSeconds(1));

        assertThat(limiter.isBlocked("e", 214)).isFalse();
    }

    @Test
    void successClearsTheCount() {
        failTimes(PinAttemptLimiter.MAX_FAILURES - 1);
        limiter.reset("e", 214);
        failTimes(1);

        assertThat(limiter.isBlocked("e", 214)).isFalse();
    }

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure("e", 214);
        }
    }

    /** A clock the test can move forward. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
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
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
