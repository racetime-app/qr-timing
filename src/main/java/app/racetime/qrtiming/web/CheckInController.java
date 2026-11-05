package app.racetime.qrtiming.web;

import app.racetime.qrtiming.api.CheckInRejectedException;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import app.racetime.qrtiming.api.RaceTimeApiException;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.QrCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The check-in pages, at the address a checkpoint QR code points to: {@code /c/{eventId}/{checkpointId}/{key}}.
 *
 * <p>The flow is:
 * <ol>
 *   <li>{@code GET}: the athlete has scanned the code. Look up the checkpoint and show the form
 *       ({@code check-in.ftlh}).</li>
 *   <li>{@code POST}: the athlete has sent the form. Check them in and show the confirmation
 *       ({@code success.ftlh}).</li>
 *   <li>If the API says no, or can't be reached, show what went wrong ({@code error.ftlh}).</li>
 * </ol>
 *
 * <p>The confirmation is rendered straight from the {@code POST}, without the usual redirect. That way the app
 * keeps no state between requests, so it runs on any number of Cloud Run instances. Sending the form twice is
 * harmless, because the API returns the first recorded time for a repeated check-in.
 */
@Controller
@RequestMapping(CheckInController.PATH)
public class CheckInController {

    private static final Logger log = LoggerFactory.getLogger(CheckInController.class);

    /** The address printed in the QR codes. */
    static final String PATH = "/c/{eventId}/{checkpointId}/{key}";

    /** How the recorded time is shown, for example {@code 14:32:08}. */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RaceTimeApiClient raceTime;

    public CheckInController(RaceTimeApiClient raceTime) {
        this.raceTime = raceTime;
    }

    /**
     * Shows the check-in form for the scanned checkpoint.
     *
     * @param bib pre-fills the bib field; set by the "Try again" button after a failed check-in
     */
    @GetMapping
    public String showForm(@PathVariable String eventId,
                           @PathVariable String checkpointId,
                           @PathVariable String key,
                           @RequestParam(name = "bib", required = false) String bib,
                           Model model,
                           HttpServletResponse response) {
        QrCode qrCode = new QrCode(eventId, checkpointId, key);
        try {
            model.addAttribute("form", new CheckInForm(bib, null));
            return showForm(qrCode, model);
        } catch (RaceTimeApiException e) {
            return showError(e, qrCode, bib, model, response);
        }
    }

    /**
     * Checks the athlete in.
     *
     * <p>{@code @Valid} checks the form against the rules in {@link CheckInForm}; problems land in
     * {@code validation} and the form is shown again with them, without calling the API.
     */
    @PostMapping
    public String checkIn(@PathVariable String eventId,
                          @PathVariable String checkpointId,
                          @PathVariable String key,
                          @Valid @ModelAttribute("form") CheckInForm form,
                          BindingResult validation,
                          Model model,
                          HttpServletResponse response) {
        QrCode qrCode = new QrCode(eventId, checkpointId, key);
        try {
            if (validation.hasErrors()) {
                response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
                return showForm(qrCode, model);
            }

            CheckInResult result = raceTime.checkIn(qrCode, form.bib(), form.pinOrNull());
            log.info("Checked in bib {} at {}", result.bib(), qrCode);

            model.addAttribute("result", result);
            model.addAttribute("recordedTime", TIME_FORMAT.format(result.localRecordedAt()));
            return "success";
        } catch (RaceTimeApiException e) {
            return showError(e, qrCode, form.bib(), model, response);
        }
    }

    /**
     * Renders the form. The event and checkpoint names are not part of the form, so they are always looked up
     * from the API, also when the form is shown again after a validation problem.
     */
    private String showForm(QrCode qrCode, Model model) {
        model.addAttribute("info", raceTime.findCheckpoint(qrCode));
        model.addAttribute("formAction", formUrl(qrCode, null));
        return "check-in";
    }

    /** Renders the error page that fits the failure. */
    private String showError(RaceTimeApiException e, QrCode qrCode, String bib, Model model,
                             HttpServletResponse response) {
        ErrorScreen screen = ErrorScreen.of(e);
        if (e instanceof CheckInRejectedException) {
            log.info("Check-in at {} rejected: {}", qrCode, screen);
        } else {
            log.warn("Check-in at {} failed: RaceTime API unavailable", qrCode, e);
        }

        model.addAttribute("screen", screen);
        if (screen.isRetryable()) {
            // Keep the bib number, so after a wrong PIN only the PIN has to be typed again.
            model.addAttribute("retryUrl", formUrl(qrCode, bib));
        }
        response.setStatus(screen.getStatus().value());
        return "error";
    }

    /** The address of the form, {@code /c/{eventId}/{checkpointId}/{key}}, optionally with {@code ?bib=...}. */
    private static String formUrl(QrCode qrCode, String bib) {
        return UriComponentsBuilder.fromPath(PATH)
                .queryParamIfPresent("bib", Optional.ofNullable(bib).filter(value -> !value.isBlank()))
                .buildAndExpand(qrCode.eventId(), qrCode.checkpointId(), qrCode.key())
                .encode()
                .toUriString();
    }
}
