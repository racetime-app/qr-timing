package app.racetime.qrtiming.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * Shows this app's own error page for errors that happen outside {@link CheckInController}, such as a mistyped
 * address (404) or an unexpected exception (500).
 *
 * <p>Spring Boot sends those to its {@code BasicErrorController}, which asks every {@link ErrorViewResolver} for a
 * page. This one answers first and renders the same {@code error.ftlh} the check-in pages use.
 */
@Component
public class QrTimingErrorViewResolver implements ErrorViewResolver, Ordered {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ErrorScreen screen = status == HttpStatus.NOT_FOUND ? ErrorScreen.NOT_FOUND : ErrorScreen.GENERIC;
        return new ModelAndView("error", Map.of("screen", screen), status);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
