package app.racetime.qrtiming.api;

import app.racetime.qrtiming.api.model.CheckInResult;
import app.racetime.qrtiming.api.model.CheckpointInfo;
import app.racetime.qrtiming.api.model.QrCode;

/**
 * Everything QR Timing needs from the RaceTime API: two calls.
 *
 * <p>Both are authorised by the {@link QrCode#key() key} inside the QR code, so the app holds no RaceTime
 * credentials of its own. The full contract, with request and response examples, is in
 * {@code docs/api-contract.md}.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link HttpRaceTimeApiClient} calls the real API.</li>
 *   <li>{@link DemoRaceTimeApiClient} answers from memory, for trying the app locally (profile {@code demo}).</li>
 * </ul>
 */
public interface RaceTimeApiClient {

    /**
     * Looks up the event and checkpoint a QR code belongs to.
     *
     * <p>{@code GET /api/v1/qr/{eventId}/{checkpointId}/{key}}
     *
     * @param qrCode the scanned code
     * @return the event name and checkpoint, to show above the check-in form
     * @throws CheckInRejectedException    with {@code QR_OUTDATED} if the code is no longer valid
     * @throws RaceTimeUnavailableException if the API could not be reached or answered unexpectedly
     */
    CheckpointInfo findCheckpoint(QrCode qrCode);

    /**
     * Checks an athlete in at the checkpoint of a QR code.
     *
     * <p>{@code POST /api/v1/qr/{eventId}/{checkpointId}/{key}/checkIn}
     *
     * <p>The API records the time when it receives the request. Repeating the call for the same athlete and
     * checkpoint is safe: the first recorded time is returned again.
     *
     * @param qrCode the scanned code
     * @param bib    the athlete's bib number (digits)
     * @param pin    the athlete's PIN, or {@code null} if none was entered. The API ignores it for athletes
     *               who have no PIN.
     * @return the recorded check-in
     * @throws CheckInRejectedException    if the API refused the check-in, for example because of a wrong PIN
     * @throws RaceTimeUnavailableException if the API could not be reached or answered unexpectedly
     */
    CheckInResult checkIn(QrCode qrCode, String bib, String pin);
}
