import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class RecommendationWebServer {
    private static final int DEFAULT_PORT = 8000;
    private static final AtomicLong WEB_RATER_COUNTER = new AtomicLong();

    private final RecommendationRunner runner;
    private final HttpServer server;
    private final String rootUrl;

    public RecommendationWebServer(int port) throws IOException {
        runner = new RecommendationRunner();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleHome);
        server.createContext("/recommend", this::handleRecommend);
        server.setExecutor(Executors.newSingleThreadExecutor());
        rootUrl = "http://localhost:" + port + "/";
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "GET");
            return;
        }

        sendHtml(exchange, 200, runner.renderRatingPage("/recommend", null, Map.of()));
    }

    private void handleRecommend(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "POST");
            return;
        }

        ArrayList<String> itemsToRate = runner.getItemsToRate();
        Map<String, Integer> submittedRatings;
        try {
            submittedRatings = parseRatings(exchange.getRequestBody(), itemsToRate);
        } catch (IllegalArgumentException exception) {
            sendHtml(
                    exchange,
                    400,
                    runner.renderRatingPage("/recommend", exception.getMessage(), Map.of())
            );
            return;
        }

        if (submittedRatings.size() < runner.getMinimumSubmittedRatings()) {
            String message = "Submit at least " + runner.getMinimumSubmittedRatings()
                    + " ratings so the recommender has enough signal to compare your taste profile.";
            sendHtml(exchange, 400, runner.renderRatingPage("/recommend", message, submittedRatings));
            return;
        }

        String webRaterId = "web-" + System.currentTimeMillis() + "-" + WEB_RATER_COUNTER.incrementAndGet();
        for (Map.Entry<String, Integer> entry : submittedRatings.entrySet()) {
            RaterDatabase.addRaterRating(webRaterId, entry.getKey(), entry.getValue());
        }

        sendHtml(exchange, 200, runner.renderRecommendationsPage(webRaterId));
    }

    private Map<String, Integer> parseRatings(InputStream requestBody, ArrayList<String> itemsToRate) throws IOException {
        String payload = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> formValues = parseForm(payload);
        Map<String, Integer> ratings = new LinkedHashMap<>();

        for (String movieId : itemsToRate) {
            String rawValue = formValues.get("rating-" + movieId);
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            int rating;
            try {
                rating = Integer.parseInt(rawValue);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Ratings must be whole numbers between 0 and 10.");
            }
            if (rating < 0 || rating > 10) {
                throw new IllegalArgumentException("Ratings must be between 0 and 10.");
            }
            ratings.put(movieId, rating);
        }

        return ratings;
    }

    private Map<String, String> parseForm(String payload) {
        Map<String, String> values = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return values;
        }

        for (String pair : payload.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            String[] keyValue = pair.split("=", 2);
            String key = decodeComponent(keyValue[0]);
            String value = keyValue.length > 1 ? decodeComponent(keyValue[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private String decodeComponent(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String allowedMethod) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethod);
        sendHtml(exchange, 405, "<h1>Method Not Allowed</h1>");
    }

    private void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private void openBrowser() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(rootUrl));
        } catch (IOException ignored) {
            // Browser launch is a convenience; the server still runs if it fails.
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        RecommendationWebServer webServer = new RecommendationWebServer(port);
        webServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
        System.out.println("Recommendation server running at " + webServer.rootUrl);
        webServer.openBrowser();
        Thread.currentThread().join();
    }
}
