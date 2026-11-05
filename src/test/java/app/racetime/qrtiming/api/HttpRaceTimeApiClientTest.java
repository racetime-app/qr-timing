package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInError;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.CheckpointInfo;
import app.racetime.qrtiming.api.model.CheckpointType;
import app.racetime.qrtiming.api.model.QrCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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
    private static final QrCode QR_CODE = new QrCode("event-1", "cp-1", "secret");

    private MockRestServiceServer server;
    private HttpRaceTimeApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpRaceTimeApiClient(builder.build());
    }

    @Test
    void findCheckpointReadsEventAndCheckpoint() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"eventName": "Spring Trail 10K",
                         "checkpoint": {"name": "Windmill turn", "type": "CHECKPOINT", "number": 3},
                         "someFutureField": true}
                        """, MediaType.APPLICATION_JSON));

        CheckpointInfo info = client.findCheckpoint(QR_CODE);

        assertThat(info.eventName()).isEqualTo("Spring Trail 10K");
        assertThat(info.checkpoint().name()).isEqualTo("Windmill turn");
        assertThat(info.checkpoint().type()).isEqualTo(CheckpointType.CHECKPOINT);
        assertThat(info.checkpoint().number()).isEqualTo(3);
        server.verify();
    }

    @Test
    void pathSegmentsAreEncoded() {
        server.expect(requestTo(API + "/api/v1/qr/event%201/cp-1/a%2Fb"))
                .andRespond(withSuccess("""
                        {"eventName": "E", "checkpoint": {"type": "FINISH"}}
                        """, MediaType.APPLICATION_JSON));

        CheckpointInfo info = client.findCheckpoint(new QrCode("event 1", "cp-1", "a/b"));

        assertThat(info.checkpoint().isFinish()).isTrue();
        server.verify();
    }

    @Test
    void checkInSendsBibAndPin() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret/checkIn"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"bib": "214", "pin": "4821"}
                        """))
                .andRespond(withSuccess("""
                        {"eventName": "Spring Trail 10K",
                         "checkpoint": {"name": "Windmill turn", "type": "CHECKPOINT", "number": 3},
                         "bib": "214",
                         "recordedAt": "2026-05-17T12:32:08.412Z",
                         "timeZone": "Europe/Warsaw"}
                        """, MediaType.APPLICATION_JSON));

        CheckInResult result = client.checkIn(QR_CODE, "214", "4821");

        assertThat(result.bib()).isEqualTo("214");
        assertThat(result.recordedAt()).isEqualTo(Instant.parse("2026-05-17T12:32:08.412Z"));
        assertThat(result.localRecordedAt().toLocalTime()).hasToString("14:32:08.412");
        server.verify();
    }

    @Test
    void checkInWithoutPinSendsNull() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret/checkIn"))
                .andExpect(content().json("""
                        {"bib": "100", "pin": null}
                        """))
                .andRespond(withSuccess("""
                        {"eventName": "E", "checkpoint": {"type": "FINISH"}, "bib": "100",
                         "recordedAt": "2026-05-17T12:32:08Z"}
                        """, MediaType.APPLICATION_JSON));

        CheckInResult result = client.checkIn(QR_CODE, "100", null);

        // No time zone from the API: the time is shown in UTC.
        assertThat(result.localRecordedAt().toLocalTime()).hasToString("12:32:08");
    }

    @Test
    void knownErrorCodeBecomesRejection() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret/checkIn"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code": "pin_mismatch", "message": "PIN does not match"}
                                """));

        assertThatThrownBy(() -> client.checkIn(QR_CODE, "214", "0000"))
                .isInstanceOfSatisfying(CheckInRejectedException.class,
                        e -> assertThat(e.error()).isEqualTo(CheckInError.PIN_MISMATCH));
    }

    @Test
    void outdatedQrCodeOnLookup() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code": "qr_outdated"}
                                """));

        assertThatThrownBy(() -> client.findCheckpoint(QR_CODE))
                .isInstanceOfSatisfying(CheckInRejectedException.class,
                        e -> assertThat(e.error()).isEqualTo(CheckInError.QR_OUTDATED));
    }

    @Test
    void unknownErrorCodeMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret/checkIn"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code": "something_new"}
                                """));

        assertThatThrownBy(() -> client.checkIn(QR_CODE, "214", null))
                .isInstanceOf(RaceTimeUnavailableException.class);
    }

    @Test
    void errorWithoutJsonBodyMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>Not Found</html>"));

        assertThatThrownBy(() -> client.findCheckpoint(QR_CODE))
                .isInstanceOf(RaceTimeUnavailableException.class);
    }

    @Test
    void serverErrorMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findCheckpoint(QR_CODE))
                .isInstanceOf(RaceTimeUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void timeoutMeansUnavailable() {
        server.expect(requestTo(API + "/api/v1/qr/event-1/cp-1/secret/checkIn"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.checkIn(QR_CODE, "214", null))
                .isInstanceOf(RaceTimeUnavailableException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }
}
