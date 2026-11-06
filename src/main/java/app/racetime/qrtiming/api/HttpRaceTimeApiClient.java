package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.Checkpoint;
import app.racetime.qrtiming.api.model.Event;
import app.racetime.qrtiming.api.model.Participant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Calls the RaceTime API over HTTP with Spring's {@link RestClient}.
 *
 * <p>This class is the heart of the integration and the part most worth copying. For each call it builds the URL
 * (the {@code RestClient} encodes the path variables), reads a {@code 2xx} JSON body into a record, and turns
 * every other answer into an exception the rest of the app understands. Network errors and timeouts become
 * {@link RaceTimeUnavailableException}.
 *
 * <p>The base URL, the API token, the timeouts and the {@code User-Agent} header are set on the {@code RestClient}
 * in {@link app.racetime.qrtiming.config.RaceTimeApiConfig}.
 */
public class HttpRaceTimeApiClient implements RaceTimeApiClient {

    static final String EVENT_PATH = "/api/v1/events/{eventId}";
    static final String CHECKPOINTS_PATH = EVENT_PATH + "/checkpoints";
    static final String PARTICIPANTS_BY_BIB_PATH = EVENT_PATH + "/participants?bib={bib}";
    static final String CHECK_IN_PATH = EVENT_PATH + "/checkIn";

    /** The JSON key under which the API reports a participant who has not started (400 Bad Request). */
    private static final String NOT_STARTED_KEY = "startTime";

    private static final ParameterizedTypeReference<List<Checkpoint>> CHECKPOINT_LIST = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Participant>> PARTICIPANT_LIST = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<Map<String, String>> ERROR_MAP = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    /**
     * @param restClient a client whose base URL points at the RaceTime API and that sends the API token
     */
    public HttpRaceTimeApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Event> findEvent(String eventId) {
        return call(() -> restClient.get()
                .uri(EVENT_PATH, eventId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }
                    return Optional.of(readBody(response, Event.class));
                }));
    }

    @Override
    public List<Checkpoint> getCheckpoints(String eventId) {
        return call(() -> restClient.get()
                .uri(CHECKPOINTS_PATH, eventId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> readBody(response, CHECKPOINT_LIST)));
    }

    @Override
    public List<Participant> findParticipantsByBib(String eventId, int bib) {
        return call(() -> restClient.get()
                .uri(PARTICIPANTS_BY_BIB_PATH, eventId, bib)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> readBody(response, PARTICIPANT_LIST)));
    }

    @Override
    public void checkIn(String eventId, String checkpointId, String participantId, Instant timestamp) {
        // The API expects ISO 8601 with millisecond precision, such as "2026-05-17T12:32:08.412Z".
        CheckInRequest body = new CheckInRequest(checkpointId, participantId,
                timestamp.truncatedTo(ChronoUnit.MILLIS).toString());

        call(() -> restClient.post()
                .uri(CHECK_IN_PATH, eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        return null;
                    }
                    if (status.isSameCodeAs(HttpStatus.CONFLICT)) {
                        throw readConflict(response);
                    }
                    if (status.isSameCodeAs(HttpStatus.BAD_REQUEST) && isNotStarted(response)) {
                        throw new CheckInRefusedException(CheckInRefusedException.Reason.NOT_STARTED, null);
                    }
                    throw unexpected(status);
                }));
    }

    /** Reads a {@code 2xx} body, or explains why there isn't one. */
    private static <T> T readBody(ConvertibleClientHttpResponse response, Class<T> type) throws IOException {
        requireSuccess(response.getStatusCode());
        return requireBody(response.bodyTo(type));
    }

    private static <T> T readBody(ConvertibleClientHttpResponse response, ParameterizedTypeReference<T> type)
            throws IOException {
        requireSuccess(response.getStatusCode());
        return requireBody(response.bodyTo(type));
    }

    private static void requireSuccess(HttpStatusCode status) {
        if (!status.is2xxSuccessful()) {
            throw unexpected(status);
        }
    }

    private static <T> T requireBody(T body) {
        if (body == null) {
            throw new RaceTimeUnavailableException("RaceTime API sent an empty response");
        }
        return body;
    }

    /**
     * An answer this app can't act on. 401 and 403 get their own message: they almost always mean the configured
     * token is wrong or does not belong to an admin, which is worth saying plainly in the logs.
     */
    private static RaceTimeUnavailableException unexpected(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return new RaceTimeUnavailableException("RaceTime API refused the API token (HTTP " + status.value()
                    + "). Check that racetime.api.token is set and belongs to an admin.");
        }
        return new RaceTimeUnavailableException("Unexpected response from RaceTime API: HTTP " + status.value());
    }

    /**
     * Reads a 409 Conflict body: {@code {"code": "checkpoint_skipped", "error": "...", "timestamp": "..."}}.
     * The {@code code} is meant for programs; the {@code error} is a message for people, which we don't parse.
     */
    private static RaceTimeApiException readConflict(ConvertibleClientHttpResponse response) {
        ConflictBody body;
        try {
            body = response.bodyTo(ConflictBody.class);
        } catch (RestClientException unreadableBody) {
            return new RaceTimeUnavailableException("Unreadable 409 response from RaceTime API", unreadableBody);
        }
        if (body == null || body.code() == null) {
            return new RaceTimeUnavailableException("RaceTime API sent 409 without a code");
        }
        return switch (body.code()) {
            case "checkpoint_skipped" ->
                    new CheckInRefusedException(CheckInRefusedException.Reason.CHECKPOINT_SKIPPED, null);
            case "already_checked_in" ->
                    new CheckInRefusedException(CheckInRefusedException.Reason.ALREADY_CHECKED_IN,
                            parseInstant(body.timestamp()));
            default -> new RaceTimeUnavailableException("Unknown conflict code from RaceTime API: " + body.code());
        };
    }

    /**
     * A 400 Bad Request names the problem field as the JSON key, such as {@code {"startTime": "Participant not
     * started."}}. {@code startTime} is the one about the athlete; any other key means this app sent a bad request.
     */
    private static boolean isNotStarted(ConvertibleClientHttpResponse response) {
        try {
            Map<String, String> errors = response.bodyTo(ERROR_MAP);
            return errors != null && errors.containsKey(NOT_STARTED_KEY);
        } catch (RestClientException unreadableBody) {
            return false;
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException unparseable) {
            return null;
        }
    }

    /**
     * Runs a request and turns transport problems into {@link RaceTimeUnavailableException}.
     *
     * <p>{@code RestClient} reports connection failures and timeouts as {@link RestClientException}
     * (more precisely, {@code ResourceAccessException}). Exceptions this class throws itself pass through.
     */
    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RaceTimeApiException alreadyHandled) {
            throw alreadyHandled;
        } catch (RestClientException transportProblem) {
            throw new RaceTimeUnavailableException("Could not reach RaceTime API", transportProblem);
        }
    }

    /**
     * JSON body of {@code POST /checkIn}.
     *
     * @param checkpoint  the checkpoint ID
     * @param participant the participant ID
     * @param timestamp   when the athlete passed, ISO 8601
     */
    record CheckInRequest(String checkpoint, String participant, String timestamp) {
    }

    /**
     * JSON body of a 409 answer to {@code POST /checkIn}.
     *
     * @param code      {@code checkpoint_skipped} or {@code already_checked_in}
     * @param timestamp for {@code already_checked_in}: when the earlier check-in was recorded, if known
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConflictBody(String code, String timestamp) {
    }
}
