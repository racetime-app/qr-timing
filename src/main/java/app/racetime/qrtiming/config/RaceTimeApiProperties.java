package app.racetime.qrtiming.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * How to reach the RaceTime API: the {@code racetime.api.*} settings in {@code application.yaml}.
 *
 * <p>On Cloud Run they are set as environment variables, such as {@code RACETIME_API_BASE_URL}. Keep the token in
 * Secret Manager and hand it to the service as {@code RACETIME_API_TOKEN}; never commit it.
 *
 * @param baseUrl        base URL of the API, such as {@code https://api.racetime.app}
 * @param token          the RaceTime API token sent as {@code X-RaceTime-Token}. It must belong to an admin.
 *                       Not needed with the {@code demo} profile.
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout    how long to wait for a response
 */
@Validated
@ConfigurationProperties("racetime.api")
public record RaceTimeApiProperties(
        @NotNull URI baseUrl,
        String token,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    /** Leaves the token out, so the settings can be logged safely. */
    @Override
    public String toString() {
        return "RaceTimeApiProperties[baseUrl=" + baseUrl + ", connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + "]";
    }
}
