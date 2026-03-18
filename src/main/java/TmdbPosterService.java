import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmdbPosterService {
    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0 Safari/537.36";
    private static final Pattern POSTER_PATH_PATTERN = Pattern.compile("\"poster_path\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PAGE_TITLE_PATTERN = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern THUMBNAIL_SOURCE_PATTERN = Pattern.compile("\"thumbnail\"\\s*:\\s*\\{[^}]*\"source\"\\s*:\\s*\"([^\"]+)\"");
    private static final String WIKIPEDIA_SEARCH_URL = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=%s&gsrlimit=1&format=json&formatversion=2";
    private static final String WIKIPEDIA_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/%s";

    private final HttpClient httpClient;
    private final Map<String, String> posterUrlCache;
    private final String apiKey;
    private final String bearerToken;

    public TmdbPosterService() {
        httpClient = HttpClient.newHttpClient();
        posterUrlCache = new ConcurrentHashMap<>();
        apiKey = normalizedEnv("TMDB_API_KEY");
        bearerToken = normalizedEnv("TMDB_BEARER_TOKEN");
    }

    public String getPosterUrl(Movie movie) {
        if (movie == null) {
            return placeholderDataUri("Movie Poster");
        }
        return posterUrlCache.computeIfAbsent(movie.getID(), ignored -> resolvePosterUrl(movie));
    }

    public String getFallbackPosterUrl(Movie movie) {
        String title = movie == null ? "Movie Poster" : movie.getTitle();
        return placeholderDataUri(title);
    }

    private String resolvePosterUrl(Movie movie) {
        if (apiKey == null && bearerToken == null) {
            return fallbackUrl(movie);
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(buildFindUrl(movie.getID())))
                    .header("Accept", "application/json")
                    .GET();

            if (bearerToken != null) {
                requestBuilder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String posterPath = extractPosterPath(response.body());
                if (posterPath != null && !posterPath.isBlank()) {
                    return TMDB_IMAGE_BASE_URL + posterPath;
                }
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return fallbackUrl(movie);
    }

    private String buildFindUrl(String imdbId) {
        String externalId = imdbId.startsWith("tt") ? imdbId : "tt" + imdbId;
        StringBuilder url = new StringBuilder("https://api.themoviedb.org/3/find/")
                .append(URLEncoder.encode(externalId, StandardCharsets.UTF_8))
                .append("?external_source=imdb_id");

        if (apiKey != null) {
            url.append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        }

        return url.toString();
    }

    private String extractPosterPath(String responseBody) {
        Matcher matcher = POSTER_PATH_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).replace("\\/", "/");
    }

    private String fallbackUrl(Movie movie) {
        String wikipediaPosterUrl = resolveWikipediaPosterUrl(movie);
        if (wikipediaPosterUrl != null) {
            return wikipediaPosterUrl;
        }
        return getFallbackPosterUrl(movie);
    }

    private String resolveWikipediaPosterUrl(Movie movie) {
        try {
            String searchTerm = movie.getTitle() + " " + movie.getYear() + " film";
            HttpResponse<String> searchResponse = sendGet(
                    String.format(WIKIPEDIA_SEARCH_URL, urlEncode(searchTerm)),
                    false
            );

            if (searchResponse == null || searchResponse.statusCode() < 200 || searchResponse.statusCode() >= 300) {
                return null;
            }

            String pageTitle = extractFirstMatch(PAGE_TITLE_PATTERN, searchResponse.body());
            if (pageTitle == null || pageTitle.isBlank()) {
                return null;
            }

            HttpResponse<String> summaryResponse = sendGet(
                    String.format(WIKIPEDIA_SUMMARY_URL, urlEncodeWikipediaTitle(pageTitle)),
                    false
            );

            if (summaryResponse == null || summaryResponse.statusCode() < 200 || summaryResponse.statusCode() >= 300) {
                return null;
            }

            String thumbnailSource = extractFirstMatch(THUMBNAIL_SOURCE_PATTERN, summaryResponse.body());
            if (thumbnailSource == null || thumbnailSource.isBlank()) {
                return null;
            }

            return thumbnailSource.replace("\\/", "/");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private HttpResponse<String> sendGet(String url, boolean includeTmdbAuth) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();

        if (includeTmdbAuth && bearerToken != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String placeholderDataUri(String title) {
        String safeTitle = title == null || title.isBlank() ? "Movie Poster" : title;
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='185' height='278' viewBox='0 0 185 278'>"
                + "<rect width='185' height='278' fill='#dfe6eb'/>"
                + "<rect x='12' y='12' width='161' height='254' rx='10' fill='#f8fafb' stroke='#a7b4be'/>"
                + "<text x='92.5' y='122' text-anchor='middle' font-size='16' font-family='Arial, sans-serif' fill='#567'>TMDb</text>"
                + "<text x='92.5' y='148' text-anchor='middle' font-size='13' font-family='Arial, sans-serif' fill='#567'>Poster unavailable</text>"
                + "<text x='92.5' y='188' text-anchor='middle' font-size='12' font-family='Arial, sans-serif' fill='#789'>"
                + escapeForXml(trimForPoster(safeTitle))
                + "</text>"
                + "</svg>";

        return "data:image/svg+xml;charset=UTF-8," + urlEncode(svg);
    }

    private String trimForPoster(String title) {
        if (title.length() <= 22) {
            return title;
        }
        return title.substring(0, 19) + "...";
    }

    private String escapeForXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String urlEncodeWikipediaTitle(String value) {
        return urlEncode(value.replace(' ', '_'));
    }

    private String extractFirstMatch(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String normalizedEnv(String envName) {
        String value = System.getenv(envName);
        if (value == null) {
            return null;
        }

        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
