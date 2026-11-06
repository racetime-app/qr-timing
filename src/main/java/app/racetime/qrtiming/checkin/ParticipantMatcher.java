package app.racetime.qrtiming.checkin;

import app.racetime.qrtiming.api.model.Participant;

import java.util.List;

/**
 * Decides which athlete is checking in, from the bib number search results and the PIN that was entered.
 *
 * <p>Bib numbers are not unique in RaceTime: categories of one event may hand out the same numbers. PINs are
 * optional: organisers give them out when a bib number alone isn't proof enough. The rules:
 * <ol>
 *   <li>No athlete has the bib number: {@link CheckInError#BIB_NOT_FOUND}.</li>
 *   <li>A PIN was entered and exactly one of the athletes has it: that athlete. Several have it:
 *       {@link CheckInError#BIB_AMBIGUOUS}.</li>
 *   <li>A PIN was entered, nobody has it, but some of the athletes have a PIN: {@link CheckInError#PIN_MISMATCH}.
 *       Picking an athlete without a PIN instead could check in the wrong person.</li>
 *   <li>Otherwise the PIN doesn't decide (none entered, or none of the athletes has one). A single athlete is
 *       checked in, unless they have a PIN that wasn't entered: {@link CheckInError#PIN_REQUIRED}. Several are
 *       {@link CheckInError#BIB_AMBIGUOUS}.</li>
 * </ol>
 * So an athlete without a PIN can type anything, or nothing, into the PIN field.
 */
public final class ParticipantMatcher {

    private ParticipantMatcher() {
    }

    /**
     * @param withBib    the athletes with the entered bib number
     * @param enteredPin the PIN the athlete typed, or {@code null} if the field was left empty
     * @return the athlete who is checking in
     * @throws CheckInRejectedException if the rules above don't lead to exactly one athlete
     */
    public static Participant match(List<Participant> withBib, String enteredPin) {
        if (withBib.isEmpty()) {
            throw new CheckInRejectedException(CheckInError.BIB_NOT_FOUND);
        }

        if (enteredPin != null) {
            List<Participant> pinMatches = withBib.stream()
                    .filter(participant -> enteredPin.equals(participant.pin()))
                    .toList();
            if (pinMatches.size() == 1) {
                return pinMatches.getFirst();
            }
            if (pinMatches.size() > 1) {
                throw new CheckInRejectedException(CheckInError.BIB_AMBIGUOUS);
            }
            if (withBib.stream().anyMatch(Participant::hasPin)) {
                throw new CheckInRejectedException(CheckInError.PIN_MISMATCH);
            }
            // Nobody with this bib has a PIN, so whatever was typed doesn't matter.
        }

        if (withBib.size() > 1) {
            throw new CheckInRejectedException(CheckInError.BIB_AMBIGUOUS);
        }
        Participant only = withBib.getFirst();
        if (only.hasPin()) {
            throw new CheckInRejectedException(CheckInError.PIN_REQUIRED);
        }
        return only;
    }
}
