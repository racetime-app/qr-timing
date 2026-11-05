package app.racetime.qrtiming.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * How to reach the RaceTime API: the {@code racetime.api.*} settings in {@code application.yaml}.
 *
 * <p>On Cloud Run they are usually set as environment variables, such as {@code RACETIME_API_BASE_URL}.
 *
 * @param baseUrl        base URL of the API, such as {@code https://api.racetime.app}
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout    how long to wait for a response
 */
@Validated
@ConfigurationProperties("racetime.api")
public record RaceTimeApiProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
