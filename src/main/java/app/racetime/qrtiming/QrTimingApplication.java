package app.racetime.qrtiming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of RaceTime QR Timing.
 *
 * <p>QR Timing lets athletes check in at a race checkpoint without timekeepers. The organiser puts up a QR code at
 * the checkpoint; the athlete scans it, enters their bib number (and a PIN if they were given one) and the check-in
 * is recorded in RaceTime.
 *
 * <p>Where to look first:
 * <ul>
 *   <li>{@link app.racetime.qrtiming.api.RaceTimeApiClient}: the two calls this app makes to the RaceTime API.</li>
 *   <li>{@link app.racetime.qrtiming.api.HttpRaceTimeApiClient}: how those calls are made over HTTP.</li>
 *   <li>{@link app.racetime.qrtiming.web.CheckInController}: the check-in pages.</li>
 * </ul>
 *
 * <p>{@code @ConfigurationPropertiesScan} picks up the {@code @ConfigurationProperties} records in the
 * {@code config} package, which hold the settings from {@code application.yaml}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class QrTimingApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrTimingApplication.class, args);
    }
}
