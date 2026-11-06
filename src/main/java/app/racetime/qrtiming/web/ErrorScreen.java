package app.racetime.qrtiming.web;

import app.racetime.qrtiming.checkin.CheckInError;
import org.springframework.http.HttpStatus;

/**
 * The variants of the error page ({@code templates/error.ftlh}).
 *
 * <p>Each variant has its own title and text in {@code messages.properties}, under {@code error.<key>.*}, and says
 * what the athlete can do next:
 * <ul>
 *   <li>{@linkplain #isRetryable() retryable}: a typo is likely, so a "Try again" button leads back to the form;</li>
 *   <li>{@linkplain #isAdviceShown() with advice}: trying again won't help, so a "What to do" box explains the next
 *       step ({@code error.<key>.advice}).</li>
 * </ul>
 * The getters follow JavaBean naming so templates can write {@code screen.key} or {@code screen.retryable}.
 */
public enum ErrorScreen {

    PIN_REQUIRED("pin-required", HttpStatus.UNPROCESSABLE_CONTENT, true, false),
    PIN_MISMATCH("pin-mismatch", HttpStatus.UNPROCESSABLE_CONTENT, true, false),
    BIB_NOT_FOUND("bib-not-found", HttpStatus.UNPROCESSABLE_CONTENT, true, false),
    BIB_AMBIGUOUS("bib-ambiguous", HttpStatus.UNPROCESSABLE_CONTENT, true, false),
    QR_OUTDATED("qr-outdated", HttpStatus.GONE, false, true),
    CHECKPOINT_SKIPPED("checkpoint-skipped", HttpStatus.UNPROCESSABLE_CONTENT, false, true),
    NOT_STARTED("not-started", HttpStatus.UNPROCESSABLE_CONTENT, false, true),
    TOO_MANY_ATTEMPTS("too-many-attempts", HttpStatus.TOO_MANY_REQUESTS, false, true),

    /** The RaceTime API could not be reached. */
    UNAVAILABLE("unavailable", HttpStatus.SERVICE_UNAVAILABLE, true, false),

    /** An address that isn't a page of this app. */
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND, false, false),

    /** Anything else that went wrong. */
    GENERIC("generic", HttpStatus.INTERNAL_SERVER_ERROR, false, false);

    private final String key;
    private final HttpStatus status;
    private final boolean retryable;
    private final boolean adviceShown;

    ErrorScreen(String key, HttpStatus status, boolean retryable, boolean adviceShown) {
        this.key = key;
        this.status = status;
        this.retryable = retryable;
        this.adviceShown = adviceShown;
    }

    /** The page to show when a check-in can't go ahead. */
    public static ErrorScreen of(CheckInError error) {
        // A switch over an enum without "default" makes the compiler check that every error has a page.
        return switch (error) {
            case QR_OUTDATED -> QR_OUTDATED;
            case BIB_NOT_FOUND -> BIB_NOT_FOUND;
            case BIB_AMBIGUOUS -> BIB_AMBIGUOUS;
            case PIN_REQUIRED -> PIN_REQUIRED;
            case PIN_MISMATCH -> PIN_MISMATCH;
            case CHECKPOINT_SKIPPED -> CHECKPOINT_SKIPPED;
            case NOT_STARTED -> NOT_STARTED;
            case TOO_MANY_ATTEMPTS -> TOO_MANY_ATTEMPTS;
        };
    }

    /** The part of the message keys for this page: {@code error.<key>.title}, {@code error.<key>.text}, ... */
    public String getKey() {
        return key;
    }

    /** The HTTP status the page is sent with. */
    public HttpStatus getStatus() {
        return status;
    }

    /** Whether the page has a "Try again" button that leads back to the form. */
    public boolean isRetryable() {
        return retryable;
    }

    /** Whether the page has a "What to do" box. */
    public boolean isAdviceShown() {
        return adviceShown;
    }
}
