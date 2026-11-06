package app.racetime.qrtiming.config;

import app.racetime.qrtiming.api.DemoRaceTimeApiClient;
import app.racetime.qrtiming.api.HttpRaceTimeApiClient;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import app.racetime.qrtiming.checkin.CheckInService;
import app.racetime.qrtiming.checkin.PinAttemptLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

/**
 * Wires the app together and chooses how it talks to RaceTime:
 * <ul>
 *   <li>Normally: {@link HttpRaceTimeApiClient}, calling the API at {@code racetime.api.base-url} with the token
 *       {@code racetime.api.token}.</li>
 *   <li>With the {@code demo} profile: {@link DemoRaceTimeApiClient}, which needs no API and no token.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class RaceTimeApiConfig {

    /** The header the RaceTime API reads API tokens from. */
    static final String TOKEN_HEADER = "X-RaceTime-Token";

    /** Sent with every request, so RaceTime can tell in its logs which integration made a call. */
    static final String USER_AGENT = "RaceTime-QR-Timing/1.0";

    /**
     * The client for the real RaceTime API.
     *
     * @param builder    Spring Boot's preconfigured builder; it already knows how to read and write JSON
     * @param properties base URL, token and timeouts from {@code application.yaml}
     */
    @Bean
    @Profile("!demo")
    RaceTimeApiClient raceTimeApiClient(RestClient.Builder builder, RaceTimeApiProperties properties) {
        // Fail at startup rather than on the first athlete's check-in.
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException("racetime.api.token is not set. Set RACETIME_API_TOKEN to a RaceTime "
                    + "API token of an admin, or run with the demo profile.");
        }

        // The JDK's own HTTP client, with explicit timeouts. Without them a slow API would leave the
        // athlete staring at a loading page.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(TOKEN_HEADER, properties.token())
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        return new HttpRaceTimeApiClient(restClient);
    }

    /** The in-memory stand-in, for running the app without the RaceTime API. */
    @Bean
    @Profile("demo")
    RaceTimeApiClient demoRaceTimeApiClient() {
        return new DemoRaceTimeApiClient();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PinAttemptLimiter pinAttemptLimiter(Clock clock) {
        return new PinAttemptLimiter(clock);
    }

    @Bean
    CheckInService checkInService(RaceTimeApiClient raceTimeApiClient, PinAttemptLimiter pinAttemptLimiter,
                                  Clock clock) {
        return new CheckInService(raceTimeApiClient, pinAttemptLimiter, clock);
    }
}
