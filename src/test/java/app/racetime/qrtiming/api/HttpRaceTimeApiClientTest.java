package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Checks the HTTP side of the integration against a fake server: the requests that are sent, and how every kind
 * of response is turned into a result or an exception.
 */
class HttpRaceTimeApiClientTest {

    private static final String API = "https://api.example.test";
    private static final String TOKEN = "test-token";

    private MockRestServiceServer server;
    private HttpRaceTimeApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API).defaultHeader("X-RaceTime-Token", TOKEN);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpRaceTimeApiClient(builder.build());
    }

    @Test
    void findEventReadsTheEventAndSendsTheToken() {
        server.expect(requestTo(API + "/api/v1/events/event-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-RaceTime-Token", TOKEN))
                .andRespond(withSuccess("""
                        {"id": "event-1", "title": "Spring Trail 10K", "timeZone": "Europe/Warsaw",
                         "timingMethod": "INTEGRATION", "eventType": "MASS_START"}
                        """, MediaType.APPLICATION_JSON));

        Optional<Event> event = client.findEvent("event-1");

        assertThat(event).hasValueSatisfying(e -> {
            assertThat(e.title()).isEqualTo("Spring Trail 10K");
            assertThat(e.timedByIntegration()).isTrue();
        });
        server.verify();
    }

    @Test
    void anUnknownEventIsEmpty() {
        server.expect(requestTo(API + "/api/v1/events/nope"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\": \"The event with ID nope not found.\"}"));

        assertThat(client.findEvent("nope")).isEmpty();
    }

    @Test
    void pathVariablesAreEncoded() {
        server.expect(requestTo(API + "/api/v1/events/a%2Fb"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findEvent("a/b")).isEmpty();
        server.verify();
    }

    @Test
    void getCheckpointsKeepsCourseOrder() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkpoints"))
                .andRespond(withSuccess("""
                        [{"id": "s", "type": "start", "name": "Start"},
                         {"id": "w", "type": "checkpoint", "name": "Windmill turn", "km": 4.5},
                         {"id": "f", "type": "finish", "isLap": false}]
                        """, MediaType.APPLICATION_JSON));

        List<Checkpoint> course = client.getCheckpoints("event-1");

        assertThat(course).extracting(Checkpoint::id).containsExactly("s", "w", "f");
        assertThat(course.get(1).isIntermediate()).isTrue();
        assertThat(course.get(2).isFinish()).isTrue();
    }

    @Test
    void findParticipantsSearchesByBib() {
        server.expect(requestTo(API + "/api/v1/events/event-1/participants?bib=214"))
                .andRespond(withSuccess("""
                        [{"id": "p1", "name": "Ann Runner", "bib": 214, "pin": "4821", "categoryId": "c1"},
                         {"id": "p2", "name": "Ann Other", "bib": 214, "pin": null}]
                        """, MediaType.APPLICATION_JSON));

        List<Participant> found = client.findParticipantsByBib("event-1", 214);

        assertThat(found).extracting(Participant::id).containsExactly("p1", "p2");
        assertThat(found.get(0).hasPin()).isTrue();
        assertThat(found.get(1).hasPin()).isFalse();
    }

    @Test
    void checkInSendsCheckpointParticipantAndTime() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"checkpoint": "cp-1", "participant": "p1", "timestamp": "2026-05-17T12:32:08.412Z"}
                        """))
                .andRespond(withSuccess());

        client.checkIn("event-1", "cp-1", "p1", Instant.parse("2026-05-17T12:32:08.412345Z"));

        server.verify();
    }

    @Test
    void notStartedIsRefused() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"startTime\": \"Participant not started.\"}"));

        assertRefused(CheckInRefusedException.Reason.NOT_STARTED, null);
    }

    @Test
    void skippedCheckpointIsRefused() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": \"checkpoint_skipped\", \"error\": \"...\"}"));

        assertRefused(CheckInRefusedException.Reason.CHECKPOINT_SKIPPED, null);
    }

    @Test
    void repeatedCheckInCarriesTheEarlierTime() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code": "already_checked_in", "error": "...",
                                 "timestamp": "2026-05-17T12:10:00.250Z"}
                                """));

        assertRefused(CheckInRefusedException.Reason.ALREADY_CHECKED_IN, Instant.parse("2026-05-17T12:10:00.250Z"));
    }

    @Test
    void unknownConflictCodeMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": \"something_new\"}"));

        assertThatThrownBy(() -> client.checkIn("event-1", "cp-1", "p1", Instant.now()))
                .isInstanceOf(RaceTimeUnavailableException.class);
    }

    @Test
    void otherBadRequestsMeanUnavailable() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkIn"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"checkpoint\": \"Checkpoint not found.\"}"));

        assertThatThrownBy(() -> client.checkIn("event-1", "cp-1", "p1", Instant.now()))
                .isInstanceOf(RaceTimeUnavailableException.class);
    }

    @Test
    void aRejectedTokenIsReportedPlainly() {
        server.expect(requestTo(API + "/api/v1/events/event-1/checkpoints"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.getCheckpoints("event-1"))
                .isInstanceOf(RaceTimeUnavailableException.class)
                .hasMessageContaining("token");
    }

    @Test
    void serverErrorMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/events/event-1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findEvent("event-1"))
                .isInstanceOf(RaceTimeUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void timeoutMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/events/event-1/participants?bib=1"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.findParticipantsByBib("event-1", 1))
                .isInstanceOf(RaceTimeUnavailableException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }

    private void assertRefused(CheckInRefusedException.Reason reason, Instant earlier) {
        assertThatThrownBy(() -> client.checkIn("event-1", "cp-1", "p1", Instant.now()))
                .isInstanceOfSatisfying(CheckInRefusedException.class, e -> {
                    assertThat(e.reason()).isEqualTo(reason);
                    assertThat(e.earlierCheckIn()).isEqualTo(earlier);
                });
    }
}
