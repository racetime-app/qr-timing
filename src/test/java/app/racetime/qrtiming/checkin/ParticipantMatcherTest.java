package app.racetime.qrtiming.checkin;

import app.racetime.qrtiming.api.model.Participant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Which athlete a bib number and a PIN point to. */
class ParticipantMatcherTest {

    private static final Participant WITH_PIN = new Participant("a", "Ann", 214, "4821");
    private static final Participant WITHOUT_PIN = new Participant("b", "Ben", 214, null);
    private static final Participant OTHER_PIN = new Participant("c", "Cleo", 214, "1111");
    private static final Participant SAME_PIN = new Participant("d", "Dan", 214, "4821");

    @Test
    void noAthleteWithTheBib() {
        assertRejected(List.of(), "4821", CheckInError.BIB_NOT_FOUND);
        assertRejected(List.of(), null, CheckInError.BIB_NOT_FOUND);
    }

    @Nested
    class OneAthlete {

        @Test
        void withTheRightPin() {
            assertThat(ParticipantMatcher.match(List.of(WITH_PIN), "4821")).isEqualTo(WITH_PIN);
        }

        @Test
        void withAWrongPin() {
            assertRejected(List.of(WITH_PIN), "0000", CheckInError.PIN_MISMATCH);
        }

        @Test
        void withThePinLeftOut() {
            assertRejected(List.of(WITH_PIN), null, CheckInError.PIN_REQUIRED);
        }

        @Test
        void withoutAPinWhateverIsTyped() {
            assertThat(ParticipantMatcher.match(List.of(WITHOUT_PIN), null)).isEqualTo(WITHOUT_PIN);
            assertThat(ParticipantMatcher.match(List.of(WITHOUT_PIN), "9999")).isEqualTo(WITHOUT_PIN);
        }
    }

    @Nested
    class SeveralAthletes {

        @Test
        void thePinPicksTheAthlete() {
            assertThat(ParticipantMatcher.match(List.of(WITH_PIN, OTHER_PIN), "1111")).isEqualTo(OTHER_PIN);
            assertThat(ParticipantMatcher.match(List.of(WITH_PIN, WITHOUT_PIN), "4821")).isEqualTo(WITH_PIN);
        }

        @Test
        void twoAthletesWithTheSamePin() {
            assertRejected(List.of(WITH_PIN, SAME_PIN), "4821", CheckInError.BIB_AMBIGUOUS);
        }

        @Test
        void aWrongPinNeverFallsBackToTheAthleteWithoutOne() {
            assertRejected(List.of(WITH_PIN, WITHOUT_PIN), "0000", CheckInError.PIN_MISMATCH);
        }

        @Test
        void noPinEntered() {
            assertRejected(List.of(WITH_PIN, WITHOUT_PIN), null, CheckInError.BIB_AMBIGUOUS);
            assertRejected(List.of(WITH_PIN, OTHER_PIN), null, CheckInError.BIB_AMBIGUOUS);
        }

        @Test
        void nobodyHasAPin() {
            Participant twin = new Participant("e", "Eve", 214, null);
            assertRejected(List.of(WITHOUT_PIN, twin), null, CheckInError.BIB_AMBIGUOUS);
            assertRejected(List.of(WITHOUT_PIN, twin), "1234", CheckInError.BIB_AMBIGUOUS);
        }
    }

    private static void assertRejected(List<Participant> withBib, String pin, CheckInError expected) {
        assertThatThrownBy(() -> ParticipantMatcher.match(withBib, pin))
                .isInstanceOfSatisfying(CheckInRejectedException.class, e -> assertThat(e.error()).isEqualTo(expected));
    }
}
