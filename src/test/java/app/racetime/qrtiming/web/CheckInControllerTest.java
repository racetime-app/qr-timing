package app.racetime.qrtiming.web;

import app.racetime.qrtiming.api.CheckInRejectedException;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import app.racetime.qrtiming.api.RaceTimeUnavailableException;
import app.racetime.qrtiming.api.model.CheckInError;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.CheckpointInfo;
import app.racetime.qrtiming.api.model.CheckpointType;
import app.racetime.qrtiming.api.model.QrCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * The check-in pages, rendered with the real templates against a mocked RaceTime API.
 */
@WebMvcTest(CheckInController.class)
class CheckInControllerTest {

    private static final String URL = "/c/event-1/cp-1/secret";
    private static final QrCode QR_CODE = new QrCode("event-1", "cp-1", "secret");

    private static final Checkpoint WINDMILL = new Checkpoint("Windmill turn", CheckpointType.CHECKPOINT, 2);
    private static final CheckpointInfo INFO = new CheckpointInfo("Spring Trail 10K", WINDMILL);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RaceTimeApiClient raceTime;

    @Test
    void showsFormForNamedCheckpoint() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(INFO);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("check-in"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(allOf(
                        containsString("Spring Trail 10K"),
                        containsString("Windmill turn"),
                        containsString("action=\"/c/event-1/cp-1/secret\""),
                        containsString("name=\"bib\""),
                        containsString("name=\"pin\""))));
    }

    @Test
    void showsNumberForCheckpointWithoutName() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(
                new CheckpointInfo("Spring Trail 10K", new Checkpoint(null, CheckpointType.CHECKPOINT, 3)));

        mvc.perform(get(URL))
                .andExpect(content().string(allOf(containsString("You are at"), containsString("Checkpoint 3"))));
    }

    @Test
    void showsFinish() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(
                new CheckpointInfo("Spring Trail 10K", new Checkpoint("Finish", CheckpointType.FINISH, null)));

        mvc.perform(get(URL))
                .andExpect(content().string(allOf(containsString("You are at the"), containsString("Finish"))));
    }

    @Test
    void prefillsBibAfterTryAgain() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(INFO);

        mvc.perform(get(URL).param("bib", "214"))
                .andExpect(content().string(containsString("value=\"214\"")));
    }

    @Test
    void outdatedCodeShowsErrorInsteadOfForm() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenThrow(new CheckInRejectedException(CheckInError.QR_OUTDATED));

        mvc.perform(get(URL))
                .andExpect(status().isGone())
                .andExpect(view().name("error"))
                .andExpect(content().string(allOf(
                        containsString("This QR code is out of date"),
                        containsString("What to do:"),
                        not(containsString("Try again")))));
    }

    @Test
    void checkInShowsRecordedTime() throws Exception {
        when(raceTime.checkIn(QR_CODE, "214", "4821")).thenReturn(new CheckInResult(
                "Spring Trail 10K", WINDMILL, "214", Instant.parse("2026-05-17T12:32:08Z"), "Europe/Warsaw"));

        mvc.perform(post(URL).param("bib", " 214 ").param("pin", "4821"))
                .andExpect(status().isOk())
                .andExpect(view().name("success"))
                .andExpect(content().string(allOf(
                        containsString("You are checked in"),
                        containsString("Windmill turn"),
                        containsString("14:32:08"))));
    }

    @Test
    void finishHasItsOwnConfirmation() throws Exception {
        Checkpoint finish = new Checkpoint(null, CheckpointType.FINISH, null);
        when(raceTime.checkIn(QR_CODE, "100", null)).thenReturn(new CheckInResult(
                "Spring Trail 10K", finish, "100", Instant.parse("2026-05-17T12:32:08Z"), null));

        mvc.perform(post(URL).param("bib", "100").param("pin", ""))
                .andExpect(content().string(allOf(containsString("Finish recorded"), containsString("12:32:08"))));
    }

    @Test
    void invalidFormIsShownAgainWithoutCallingTheApi() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(INFO);

        mvc.perform(post(URL).param("bib", "").param("pin", "12ab"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(view().name("check-in"))
                .andExpect(content().string(allOf(
                        containsString("Enter your bib number."),
                        containsString("A PIN has digits only."),
                        containsString("aria-invalid=\"true\""))));

        verify(raceTime, never()).checkIn(any(), anyString(), any());
    }

    @ParameterizedTest
    @EnumSource(CheckInError.class)
    void everyRejectionHasAnErrorPage(CheckInError error) throws Exception {
        when(raceTime.checkIn(eq(QR_CODE), eq("214"), isNull())).thenThrow(new CheckInRejectedException(error));
        ErrorScreen screen = ErrorScreen.of(error);

        mvc.perform(post(URL).param("bib", "214"))
                .andExpect(status().is(screen.getStatus().value()))
                .andExpect(view().name("error"))
                .andExpect(content().string(screen.isRetryable()
                        ? containsString("href=\"/c/event-1/cp-1/secret?bib=214\"")
                        : not(containsString("Try again"))));
    }

    @Test
    void unavailableApiOffersRetry() throws Exception {
        when(raceTime.checkIn(eq(QR_CODE), eq("214"), isNull()))
                .thenThrow(new RaceTimeUnavailableException("down"));

        mvc.perform(post(URL).param("bib", "214"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(allOf(
                        containsString("RaceTime is not responding"),
                        containsString("Try again"))));
    }

    @Test
    void securityHeadersAreSet() throws Exception {
        when(raceTime.findCheckpoint(QR_CODE)).thenReturn(INFO);

        mvc.perform(get(URL))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }
}
