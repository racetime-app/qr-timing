package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInError;
import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.CheckpointInfo;
import app.racetime.qrtiming.api.model.QrCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Calls the RaceTime API over HTTP with Spring's {@link RestClient}.
 *
 * <p>This class is the heart of the integration and the part most worth copying. For each call it:
 * <ol>
 *   <li>builds the URL from the three parts of the QR code (the {@code RestClient} encodes them);</li>
 *   <li>on {@code 2xx}, reads the JSON body into a record;</li>
 *   <li>on {@code 4xx} with a known error {@code code}, throws {@link CheckInRejectedException};</li>
 *   <li>on anything else, including network errors and timeouts, throws {@link RaceTimeUnavailableException}.</li>
 * </ol>
 *
 * <p>The base URL, timeouts and the {@code User-Agent} header are set on the {@code RestClient} in
 * {@link app.racetime.qrtiming.config.RaceTimeApiConfig}.
 */
public class HttpRaceTimeApiClient implements RaceTimeApiClient {

    /** Path of the checkpoint lookup. The {@code RestClient} fills in and encodes the {@code {...}} variables. */
    static final String CHECKPOINT_PATH = "/api/v1/qr/{eventId}/{checkpointId}/{key}";

    /** Path of the check-in. */
    static final String CHECK_IN_PATH = CHECKPOINT_PATH + "/checkIn";

    private final RestClient restClient;

    /**
     * @param restClient a client whose base URL points at the RaceTime API, such as {@code https://api.racetime.app}
     */
    public HttpRaceTimeApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CheckpointInfo findCheckpoint(QrCode qrCode) {
        return call(() -> restClient.get()
                .uri(CHECKPOINT_PATH, qrCode.eventId(), qrCode.checkpointId(), qrCode.key())
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> readResponse(response, CheckpointInfo.class)));
    }

    @Override
    public CheckInResult checkIn(QrCode qrCode, String bib, String pin) {
        return call(() -> restClient.post()
                .uri(CHECK_IN_PATH, qrCode.eventId(), qrCode.checkpointId(), qrCode.key())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new CheckInRequest(bib, pin))
                .exchange((request, response) -> readResponse(response, CheckInResult.class)));
    }

    /**
     * Turns an HTTP response into a result or one of this app's exceptions.
     *
     * <p>{@code exchange(...)} is used instead of {@code retrieve()} because it hands over the raw response for
     * every status, which makes the handling of each case easy to read in one place.
     */
    private static <T> T readResponse(ConvertibleClientHttpResponse response, Class<T> type) throws IOException {
        HttpStatusCode status = response.getStatusCode();

        if (status.is2xxSuccessful()) {
            T body = response.bodyTo(type);
            if (body == null) {
                throw new RaceTimeUnavailableException("RaceTime API sent an empty response");
            }
            return body;
        }

        if (status.is4xxClientError()) {
            Optional<CheckInError> error = readErrorCode(response);
            if (error.isPresent()) {
                throw new CheckInRejectedException(error.get());
            }
        }

        throw new RaceTimeUnavailableException("Unexpected response from RaceTime API: HTTP " + status.value());
    }

    /**
     * Reads the {@code code} from an error body such as {@code {"code": "pin_mismatch"}}.
     *
     * @return the error, or empty if the body is missing, is not JSON, or holds a code this app does not know
     */
    private static Optional<CheckInError> readErrorCode(ConvertibleClientHttpResponse response) {
        try {
            ApiError body = response.bodyTo(ApiError.class);
            return body == null ? Optional.empty() : CheckInError.fromCode(body.code());
        } catch (RestClientException unreadableBody) {
            return Optional.empty();
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
     * JSON body of the check-in request.
     *
     * @param bib the athlete's bib number
     * @param pin the athlete's PIN, or {@code null} when none was entered
     */
    record CheckInRequest(String bib, String pin) {
    }

    /**
     * JSON body of an error response.
     *
     * @param code a machine-readable reason, see {@link CheckInError}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiError(String code) {
    }
}
