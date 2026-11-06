package app.racetime.qrtiming.web;

import app.racetime.qrtiming.api.RaceTimeUnavailableException;
import app.racetime.qrtiming.checkin.CheckInError;
import app.racetime.qrtiming.checkin.CheckInRejectedException;
import app.racetime.qrtiming.checkin.CheckInResult;
import app.racetime.qrtiming.checkin.CheckInService;
import app.racetime.qrtiming.checkin.CheckpointInfo;
import app.racetime.qrtiming.checkin.CheckpointView;
import app.racetime.qrtiming.checkin.QrCode;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
 * The check-in pages, rendered with the real templates against a mocked {@link CheckInService}.
 */
@WebMvcTest(CheckInController.class)
class CheckInControllerTest {

    private static final String URL = "/c/event-1/cp-1";
    private static final QrCode QR_CODE = new QrCode("event-1", "cp-1");

    private static final CheckpointView WINDMILL = new CheckpointView("Windmill turn", false, 1);
    private static final CheckpointView FINISH = new CheckpointView("Finish", true, null);
    private static final Instant RECORDED = Instant.parse("2026-05-17T12:32:08Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CheckInService checkIns;

    @Test
    void showsFormForNamedCheckpoint() throws Exception {
        givenCheckpoint(WINDMILL);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("check-in"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(allOf(
                        containsString("Spring Trail 10K"),
                        containsString("Windmill turn"),
                        containsString("action=\"/c/event-1/cp-1\""),
                        containsString("name=\"bib\""),
                        containsString("name=\"pin\""))));
    }

    @Test
    void showsNumberForCheckpointWithoutName() throws Exception {
        givenCheckpoint(new CheckpointView(null, false, 2));

        mvc.perform(get(URL))
                .andExpect(content().string(allOf(containsString("You are at"), containsString("Checkpoint 2"))));
    }

    @Test
    void showsFinish() throws Exception {
        givenCheckpoint(FINISH);

        mvc.perform(get(URL))
                .andExpect(content().string(allOf(containsString("You are at the"), containsString("Finish"))));
    }

    @Test
    void prefillsBibAfterTryAgain() throws Exception {
        givenCheckpoint(WINDMILL);

        mvc.perform(get(URL).param("bib", "214"))
                .andExpect(content().string(containsString("value=\"214\"")));
    }

    @Test
    void outdatedCodeShowsErrorInsteadOfForm() throws Exception {
        when(checkIns.findCheckpoint(QR_CODE)).thenThrow(new CheckInRejectedException(CheckInError.QR_OUTDATED));

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
        when(checkIns.checkIn(QR_CODE, 214, "4821")).thenReturn(result(WINDMILL, RECORDED, false));

        mvc.perform(post(URL).param("bib", " 214 ").param("pin", "4821"))
                .andExpect(status().isOk())
                .andExpect(view().name("success"))
                .andExpect(content().string(allOf(
                        containsString("You are checked in"),
                        containsString("Windmill turn"),
                        containsString("14:32:08"))));
    }

    @Test
    void anEmptyPinIsSentAsNone() throws Exception {
        when(checkIns.checkIn(QR_CODE, 100, null)).thenReturn(result(FINISH, RECORDED, false));

        mvc.perform(post(URL).param("bib", "100").param("pin", ""))
                .andExpect(content().string(allOf(containsString("Finish recorded"), containsString("14:32:08"))));
    }

    @Test
    void aRepeatedCheckInSaysSo() throws Exception {
        when(checkIns.checkIn(QR_CODE, 214, "4821")).thenReturn(result(WINDMILL, RECORDED, true));

        mvc.perform(post(URL).param("bib", "214").param("pin", "4821"))
                .andExpect(content().string(allOf(containsString("You are already checked in"),
                        containsString("14:32:08"))));
    }

    @Test
    void aRepeatedCheckInWithoutAKnownTimeHidesTheTime() throws Exception {
        when(checkIns.checkIn(QR_CODE, 214, "4821")).thenReturn(result(FINISH, null, true));

        mvc.perform(post(URL).param("bib", "214").param("pin", "4821"))
                .andExpect(content().string(allOf(containsString("Finish already recorded"),
                        not(containsString("Time recorded")))));
    }

    @Test
    void invalidFormIsShownAgainWithoutCheckingIn() throws Exception {
        givenCheckpoint(WINDMILL);

        mvc.perform(post(URL).param("bib", "").param("pin", "12ab"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(view().name("check-in"))
                .andExpect(content().string(allOf(
                        containsString("Enter your bib number."),
                        containsString("A PIN has digits only."),
                        containsString("aria-invalid=\"true\""))));

        verify(checkIns, never()).checkIn(any(), anyInt(), any());
    }

    @ParameterizedTest
    @EnumSource(CheckInError.class)
    void everyRejectionHasAnErrorPage(CheckInError error) throws Exception {
        when(checkIns.checkIn(QR_CODE, 214, null)).thenThrow(new CheckInRejectedException(error));
        ErrorScreen screen = ErrorScreen.of(error);

        mvc.perform(post(URL).param("bib", "214"))
                .andExpect(status().is(screen.getStatus().value()))
                .andExpect(view().name("error"))
                .andExpect(content().string(screen.isRetryable()
                        ? containsString("href=\"/c/event-1/cp-1?bib=214\"")
                        : not(containsString("Try again"))));
    }

    @Test
    void unavailableApiOffersRetry() throws Exception {
        when(checkIns.checkIn(QR_CODE, 214, null)).thenThrow(new RaceTimeUnavailableException("down"));

        mvc.perform(post(URL).param("bib", "214"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(allOf(
                        containsString("RaceTime is not responding"),
                        containsString("Try again"))));
    }

    @Test
    void securityHeadersAreSet() throws Exception {
        givenCheckpoint(WINDMILL);

        mvc.perform(get(URL))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }

    private void givenCheckpoint(CheckpointView checkpoint) {
        when(checkIns.findCheckpoint(QR_CODE)).thenReturn(new CheckpointInfo("Spring Trail 10K", checkpoint));
    }

    private static CheckInResult result(CheckpointView checkpoint, Instant recordedAt, boolean repeated) {
        return new CheckInResult("Spring Trail 10K", checkpoint, "214", recordedAt, "Europe/Warsaw", repeated);
    }
}
