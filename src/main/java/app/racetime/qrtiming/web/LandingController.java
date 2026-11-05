package app.racetime.qrtiming.web;

import app.racetime.qrtiming.config.QrTimingProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The landing page at {@code /}.
 *
 * <p>It only explains what QR Timing is. People arrive here by opening the site without a QR code, or by
 * following "What is this?" from the check-in page.
 */
@Controller
public class LandingController {

    private final QrTimingProperties properties;

    public LandingController(QrTimingProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("racetimeAppUrl", properties.racetimeAppUrl());
        return "landing";
    }
}
