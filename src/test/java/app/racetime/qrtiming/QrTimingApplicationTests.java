package app.racetime.qrtiming;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole app with the demo API and walks through the pages, as an athlete would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class QrTimingApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void landingPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Check in at the checkpoint with your phone."),
                        containsString("href=\"https://racetime.app/get-app?utm_source=qr_timing\""))));
    }

    @Test
    void checkInWithDemoAthlete() throws Exception {
        mvc.perform(get("/c/demo/windmill"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Windmill turn")));

        mvc.perform(post("/c/demo/windmill").param("bib", "214").param("pin", "4821"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You are checked in")));

        // Checking in again shows the confirmation again, and skipping ahead is refused.
        mvc.perform(post("/c/demo/windmill").param("bib", "214").param("pin", "4821"))
                .andExpect(content().string(containsString("You are already checked in")));
        mvc.perform(post("/c/demo/finish").param("bib", "214").param("pin", "4821"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(containsString("A checkpoint was missed")));
    }

    @Test
    void unknownAddressShowsNotFoundPage() throws Exception {
        mvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound());

        // A real server forwards a 404 to Spring Boot's /error page. MockMvc doesn't, so do that step by hand.
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/no-such-page")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void unexpectedErrorShowsGenericPage() throws Exception {
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Something went wrong")));
    }

    @Test
    void stylesheetIsServed() throws Exception {
        mvc.perform(get("/static/css/qr-timing.css"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointForCloudRun() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UP")));
    }
}
