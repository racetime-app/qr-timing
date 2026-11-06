package app.racetime.qrtiming.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the athlete typed into the check-in form.
 *
 * <p>Only the format is checked here: digits, and a sensible length. Whether the bib exists and the PIN matches is
 * up to the RaceTime API.
 *
 * <p>Error messages come from {@code messages.properties}. Spring looks them up by
 * {@code <constraint>.<form name>.<field>}, for example {@code NotBlank.form.bib}.
 *
 * @param bib the bib number; required
 * @param pin the PIN; optional, because many athletes have none
 */
public record CheckInForm(
        @NotBlank @Pattern(regexp = "\\d*") @Size(max = 9) String bib,
        @Pattern(regexp = "\\d*") @Size(max = 12) String pin) {

    /** Removes spaces around the values, which phones sometimes add. */
    public CheckInForm {
        bib = bib == null ? null : bib.strip();
        pin = pin == null ? null : pin.strip();
    }

    /** An empty form, for when the page opens. */
    static CheckInForm empty() {
        return new CheckInForm(null, null);
    }

    /** The bib as a number. Only valid after validation: at most 9 digits always fit in an {@code int}. */
    int bibNumber() {
        return Integer.parseInt(bib);
    }

    /** The PIN that was entered: {@code null} when the field was left empty. */
    String pinOrNull() {
        return pin == null || pin.isEmpty() ? null : pin;
    }
}
