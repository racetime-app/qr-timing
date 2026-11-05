package app.racetime.qrtiming.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds browser security headers to every response.
 *
 * <p>The app has no JavaScript, no accounts and no cookies, so a few headers cover what matters:
 * <ul>
 *   <li>{@code Content-Security-Policy}: only this app's own files, plus Google Fonts, may load.</li>
 *   <li>{@code Referrer-Policy: no-referrer}: check-in URLs hold the secret QR key, so the browser must not pass
 *       them on to other sites (such as the fonts server, or racetime.app when a link is followed).</li>
 *   <li>{@code Cache-Control: no-store} on check-in pages: they show personal results, and are never cached.</li>
 * </ul>
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src https://fonts.gstatic.com",
            "img-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'none'");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (request.getRequestURI().startsWith("/c/")) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        }
        chain.doFilter(request, response);
    }
}
