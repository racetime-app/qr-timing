package app.racetime.qrtiming.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * Settings of the pages themselves: the {@code qr-timing.*} settings in {@code application.yaml}.
 *
 * @param racetimeAppUrl where the "Open RaceTime" links on the landing page point to
 */
@Validated
@ConfigurationProperties("qr-timing")
public record QrTimingProperties(@NotNull URI racetimeAppUrl) {
}
