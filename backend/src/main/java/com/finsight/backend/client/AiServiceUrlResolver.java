package com.finsight.backend.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.lang.NonNull;

/**
 * Produces a stable HTTP base URL for the local FastAPI service.
 *
 * FastAPI/Uvicorn is started locally with plain HTTP. In particular, an
 * environment variable left over from another project can accidentally use
 * https://localhost:8000, which makes Uvicorn report "Invalid HTTP request
 * received" because it receives TLS bytes on an HTTP socket.
 */
public final class AiServiceUrlResolver {

    private AiServiceUrlResolver() {
    }

    /**
     * Clean the configured URL: blank -> http://127.0.0.1:8000, trailing slashes
     * removed, and any localhost-style address is rewritten to plain
     * http://127.0.0.1:<port>. Non-local URLs (a deployed AI service) are returned as
     * configured.
     */
    @NonNull
    public static String resolve(String configuredUrl) {
        String url = configuredUrl == null ? "" : configuredUrl.trim();
        if (url.isBlank()) {
            return "http://127.0.0.1:8000";
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && isLoopback(host)) {
                int port = uri.getPort() > 0 ? uri.getPort() : 8000;
                // Always use the loopback IPv4 address for local AI service calls.
                // This avoids accidental HTTPS/TLS and localhost IPv6/proxy issues.
                return "http://127.0.0.1:" + port;
            }
        } catch (URISyntaxException ignored) {
            // Fall back to the sanitized string below so configuration errors are
            // reported by RestClient instead of being silently swallowed here.
        }

        return url;
    }

    /**
     * True for localhost, 127.0.0.1, 0.0.0.0 and ::1.
     */
    private static boolean isLoopback(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("0.0.0.0")
                || normalized.equals("::1");
    }
}
