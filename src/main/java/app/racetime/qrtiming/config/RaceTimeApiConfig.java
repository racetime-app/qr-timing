package app.racetime.qrtiming.config;

import app.racetime.qrtiming.api.DemoRaceTimeApiClient;
import app.racetime.qrtiming.api.HttpRaceTimeApiClient;
import app.racetime.qrtiming.api.RaceTimeApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Chooses how the app talks to RaceTime.
 *
 * <ul>
 *   <li>Normally: {@link HttpRaceTimeApiClient}, calling the API at {@code racetime.api.base-url}.</li>
 *   <li>With the {@code demo} profile: {@link DemoRaceTimeApiClient}, which needs no API at all.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class RaceTimeApiConfig {

    /** Sent with every request, so RaceTime can tell in its logs which integration made a call. */
    static final String USER_AGENT = "RaceTime-QR-Timing/1.0";

    /**
     * The client for the real RaceTime API.
     *
     * @param builder    Spring Boot's preconfigured builder; it already knows how to read and write JSON
     * @param properties base URL and timeouts from {@code application.yaml}
     */
    @Bean
    @Profile("!demo")
    RaceTimeApiClient raceTimeApiClient(RestClient.Builder builder, RaceTimeApiProperties properties) {
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
}
