import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecommendationRunner implements Recommender {
    private static final String MOVIE_FILE = "ratedmoviesfull.csv";
    private static final String RATING_FILE = "ratings.csv";
    private static final int ITEMS_TO_RATE = 12;
    private static final int MINIMUM_SUBMITTED_RATINGS = 5;
    private static final int MINIMAL_RATERS = 5;
    private static final int SIMILAR_RATERS = 20;
    private static final int MAX_RECOMMENDATIONS = 10;
    private final TmdbPosterService tmdbPosterService = new TmdbPosterService();

    @Override
    public ArrayList<String> getItemsToRate() {
        initializeDatabases();

        FourthRatings fourthRatings = new FourthRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new YearAfterFilter(1980));
        filters.addFilter(new MinutesFilter(80, 180));

        ArrayList<Rating> candidates = fourthRatings.getAverageRatingsByFilter(50, filters);
        candidates.sort(Collections.reverseOrder());

        ArrayList<String> items = new ArrayList<>();
        Set<String> primaryDirectors = new HashSet<>();

        for (Rating rating : candidates) {
            Movie movie = MovieDatabase.getMovie(rating.getItem());
            if (movie == null) {
                continue;
            }

            String primaryDirector = movie.getDirector().split(",")[0].trim();
            if (items.size() < 6 || primaryDirectors.add(primaryDirector)) {
                items.add(movie.getID());
            }

            if (items.size() == ITEMS_TO_RATE) {
                return items;
            }
        }

        for (String movieId : MovieDatabase.filterBy(new TrueFilter())) {
            Movie movie = MovieDatabase.getMovie(movieId);
            if (movie != null && !items.contains(movieId)) {
                items.add(movieId);
            }
            if (items.size() == ITEMS_TO_RATE) {
                break;
            }
        }

        return items;
    }

    @Override
    public void printRecommendationsFor(String webRaterID) {
        System.out.print(renderRecommendationsContent(webRaterID));
    }

    public String renderRatingPage(String formAction, String errorMessage, Map<String, Integer> selectedRatings) {
        initializeDatabases();

        StringBuilder html = new StringBuilder();
        html.append(documentStart("Rate Movies"));
        html.append(buildStyles());
        html.append("<main class=\"page\">");
        html.append("<section class=\"hero\">");
        html.append("<p class=\"eyebrow\">Personalized discovery</p>");
        html.append("<h2>Rate a few movies</h2>");
        html.append("<p class=\"intro\">Pick at least ")
                .append(MINIMUM_SUBMITTED_RATINGS)
                .append(" titles on the 0 to 10 scale. The recommender will compare your taste profile with similar viewers and return a ranked table of movies to watch next.</p>");
        html.append("</section>");

        if (errorMessage != null && !errorMessage.isBlank()) {
            html.append(buildStateCard("More ratings needed", errorMessage));
        }

        html.append("<form class=\"rating-form\" method=\"post\" action=\"")
                .append(escapeHtml(formAction))
                .append("\">");
        html.append("<section class=\"rating-grid\">");

        for (String movieId : getItemsToRate()) {
            Movie movie = MovieDatabase.getMovie(movieId);
            if (movie == null) {
                continue;
            }

            Integer selectedRating = selectedRatings == null ? null : selectedRatings.get(movieId);
            String posterUrl = tmdbPosterService.getPosterUrl(movie);
            String fallbackPosterUrl = tmdbPosterService.getFallbackPosterUrl(movie);
            html.append("<article class=\"rating-card\">");
            html.append("<div class=\"poster-wrap\">");
            html.append("<img src=\"")
                    .append(escapeHtml(posterUrl))
                    .append("\" onerror=\"this.onerror=null;this.src='")
                    .append(escapeHtmlAttribute(fallbackPosterUrl))
                    .append("';\" alt=\"")
                    .append(escapeHtml(movie.getTitle()))
                    .append(" poster\">");
            html.append("</div>");
            html.append("<div class=\"card-copy\">");
            html.append("<h3>").append(escapeHtml(movie.getTitle())).append("</h3>");
            html.append("<p class=\"card-meta\">")
                    .append(movie.getYear())
                    .append(" | ")
                    .append(movie.getMinutes())
                    .append(" min</p>");
            html.append("<p class=\"card-detail\"><strong>Genres</strong><span>")
                    .append(escapeHtml(movie.getGenres()))
                    .append("</span></p>");
            html.append("<p class=\"card-detail\"><strong>Director</strong><span>")
                    .append(escapeHtml(movie.getDirector()))
                    .append("</span></p>");
            html.append("<label class=\"rating-label\" for=\"rating-")
                    .append(escapeHtmlAttribute(movieId))
                    .append("\">Your rating</label>");
            html.append("<select id=\"rating-")
                    .append(escapeHtmlAttribute(movieId))
                    .append("\" name=\"rating-")
                    .append(escapeHtmlAttribute(movieId))
                    .append("\">");
            html.append("<option value=\"\">Not rated</option>");
            for (int value = 10; value >= 0; value--) {
                html.append("<option value=\"")
                        .append(value)
                        .append("\"");
                if (selectedRating != null && selectedRating == value) {
                    html.append(" selected");
                }
                html.append(">")
                        .append(value)
                        .append(" / 10</option>");
            }
            html.append("</select>");
            html.append("</div>");
            html.append("</article>");
        }

        html.append("</section>");
        html.append("<div class=\"form-actions\">");
        html.append("<p class=\"note\">Leave any movie blank if you have not seen it. Submitting ")
                .append(MINIMUM_SUBMITTED_RATINGS)
                .append(" or more ratings usually produces the strongest matches.</p>");
        html.append("<button class=\"primary-button\" type=\"submit\">Get recommendations</button>");
        html.append("</div>");
        html.append("</form>");
        html.append("</main>");
        html.append(documentEnd());
        return html.toString();
    }

    public String renderRecommendationsPage(String webRaterID) {
        return documentStart("Recommended Movies")
                + renderRecommendationsContent(webRaterID)
                + documentEnd();
    }

    public int getMinimumSubmittedRatings() {
        return MINIMUM_SUBMITTED_RATINGS;
    }

    private String renderRecommendationsContent(String webRaterID) {
        initializeDatabases();

        StringBuilder html = new StringBuilder();
        html.append(buildStyles());
        html.append("<main class=\"page\">");

        Rater webRater = RaterDatabase.getRater(webRaterID);
        if (webRater == null) {
            html.append(buildStateCard(
                    "Ratings not found",
                    "Unable to find ratings for user " + escapeHtml(webRaterID) + "."
            ));
            html.append("</main>");
            return html.toString();
        }

        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> rawRecommendations = fourthRatings.getSimilarRatingsByFilter(
                webRaterID,
                SIMILAR_RATERS,
                MINIMAL_RATERS,
                new TrueFilter()
        );

        ArrayList<Rating> recommendations = new ArrayList<>();
        Set<String> ratedMovieIds = new HashSet<>(webRater.getItemsRated());
        for (Rating recommendation : rawRecommendations) {
            if (!ratedMovieIds.contains(recommendation.getItem())) {
                recommendations.add(recommendation);
            }
            if (recommendations.size() == MAX_RECOMMENDATIONS) {
                break;
            }
        }

        if (recommendations.isEmpty()) {
            html.append(buildStateCard(
                    "No recommendations yet",
                    "Try rating a few more movies to generate stronger matches."
            ));
            html.append("<p class=\"back-link\"><a class=\"secondary-link\" href=\"/\">Rate another set of movies</a></p>");
            html.append("</main>");
            return html.toString();
        }

        html.append("<section class=\"hero\">");
        html.append("<p class=\"eyebrow\">Personalized discovery</p>");
        html.append("<h2>Recommended Movies</h2>");
        html.append("<p class=\"intro\">Weighted picks based on viewers whose rating patterns are closest to yours. You rated ")
                .append(webRater.numRatings())
                .append(" movies, and the table below excludes anything you already scored.</p>");
        html.append("</section>");
        html.append("<div class=\"table-shell\">");
        html.append("<table>");
        html.append("<thead><tr><th>#</th><th>Poster</th><th>Movie</th><th>Details</th><th>Score</th></tr></thead>");
        html.append("<tbody>");

        for (int index = 0; index < recommendations.size(); index++) {
            Rating recommendation = recommendations.get(index);
            String movieId = recommendation.getItem();
            Movie movie = MovieDatabase.getMovie(movieId);
            if (movie == null) {
                continue;
            }

            String imdbLink = "https://www.imdb.com/title/tt" + movie.getID();
            String posterUrl = tmdbPosterService.getPosterUrl(movie);
            String fallbackPosterUrl = tmdbPosterService.getFallbackPosterUrl(movie);
            html.append("<tr>");
            html.append("<td class=\"rank-cell\"><span class=\"rank-badge\">")
                    .append(index + 1)
                    .append("</span></td>");
            html.append("<td class=\"poster-cell\"><img src=\"")
                    .append(escapeHtml(posterUrl))
                    .append("\" onerror=\"this.onerror=null;this.src='")
                    .append(escapeHtmlAttribute(fallbackPosterUrl))
                    .append("';\" alt=\"")
                    .append(escapeHtml(movie.getTitle()))
                    .append(" poster\"></td>");
            html.append("<td class=\"movie-cell\"><a class=\"movie-link\" href=\"")
                    .append(escapeHtml(imdbLink))
                    .append("\">")
                    .append(escapeHtml(movie.getTitle()))
                    .append("</a><br><span class=\"meta\">")
                    .append(movie.getYear())
                    .append("</span></td>");
            html.append("<td class=\"details-cell\">")
                    .append("<div class=\"details-row\"><strong>Genres</strong><span>")
                    .append(escapeHtml(movie.getGenres()))
                    .append("</span></div>")
                    .append("<div class=\"details-row\"><strong>Directors</strong><span>")
                    .append(escapeHtml(movie.getDirector()))
                    .append("</span></div>")
                    .append("<div class=\"details-row\"><strong>Country</strong><span>")
                    .append(escapeHtml(movie.getCountry()))
                    .append("</span></div>")
                    .append("<div class=\"details-row\"><strong>Minutes</strong><span>")
                    .append(movie.getMinutes())
                    .append("</span></div>")
                    .append("</td>");
            html.append("<td class=\"score-cell\"><span class=\"score-pill\">")
                    .append(String.format(Locale.US, "%.2f", recommendation.getValue()))
                    .append("</span></td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        html.append("<p class=\"note\">Scores are weighted averages from users with the most similar rating patterns.</p>");
        html.append("<p class=\"back-link\"><a class=\"secondary-link\" href=\"/\">Rate another set of movies</a></p>");
        html.append("</main>");
        return html.toString();
    }

    private void initializeDatabases() {
        MovieDatabase.initialize(MOVIE_FILE);
        RaterDatabase.initialize(RATING_FILE);
    }

    private String buildStateCard(String title, String message) {
        return "<section class=\"state-card\">"
                + "<p class=\"eyebrow\">Personalized discovery</p>"
                + "<h2>" + title + "</h2>"
                + "<p>" + message + "</p>"
                + "</section>";
    }

    private String documentStart(String title) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Space+Grotesk:wght@500;700&display=swap" rel="stylesheet">
                </head>
                <body>
                """.formatted(escapeHtml(title));
    }

    private String documentEnd() {
        return """
                </body>
                </html>
                """;
    }

    private String buildStyles() {
        return """
                <style>
                :root {
                    --bg: #f4efe8;
                    --panel: rgba(255, 252, 247, 0.92);
                    --panel-strong: #fffdf9;
                    --ink: #1a2233;
                    --muted: #5f6777;
                    --line: rgba(26, 34, 51, 0.12);
                    --accent: #0f766e;
                    --accent-soft: #e0f2ef;
                    --warm: #c57f2f;
                    --warm-soft: #fff2de;
                    --shadow: 0 22px 60px rgba(46, 51, 63, 0.12);
                }

                * {
                    box-sizing: border-box;
                }

                body {
                    margin: 0;
                    min-height: 100vh;
                    font-family: "Manrope", "Segoe UI", sans-serif;
                    color: var(--ink);
                    background:
                            radial-gradient(circle at top left, rgba(15, 118, 110, 0.14), transparent 36%),
                            radial-gradient(circle at top right, rgba(197, 127, 47, 0.14), transparent 28%),
                            var(--bg);
                }

                .page {
                    max-width: 1180px;
                    margin: 0 auto;
                    padding: 48px 20px 64px;
                }

                .hero,
                .table-shell,
                .state-card,
                .rating-card,
                .form-actions {
                    border: 1px solid var(--line);
                    box-shadow: var(--shadow);
                }

                .hero,
                .state-card,
                .rating-card,
                .form-actions {
                    background: var(--panel);
                    border-radius: 28px;
                }

                .hero {
                    padding: 32px;
                    margin-bottom: 24px;
                }

                .eyebrow {
                    margin: 0 0 10px;
                    font-size: 0.78rem;
                    letter-spacing: 0.18em;
                    text-transform: uppercase;
                    font-weight: 800;
                    color: var(--accent);
                }

                h2 {
                    margin: 0;
                    color: var(--ink);
                    font-family: "Space Grotesk", "Segoe UI", sans-serif;
                    font-size: clamp(2rem, 4vw, 3.25rem);
                    line-height: 0.98;
                }

                h3 {
                    margin: 0;
                    font-family: "Space Grotesk", "Segoe UI", sans-serif;
                    font-size: 1.18rem;
                    line-height: 1.2;
                }

                .intro,
                .note,
                .state-card p,
                .card-meta,
                .card-detail span {
                    color: var(--muted);
                    line-height: 1.7;
                }

                .intro {
                    max-width: 760px;
                    margin: 14px 0 0;
                    font-size: 1rem;
                }

                .rating-form {
                    display: grid;
                    gap: 24px;
                }

                .rating-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
                    gap: 20px;
                }

                .rating-card {
                    overflow: hidden;
                }

                .poster-wrap {
                    padding: 18px 18px 0;
                }

                .card-copy {
                    padding: 18px;
                }

                .card-meta {
                    margin: 10px 0 14px;
                    font-size: 0.92rem;
                    font-weight: 700;
                }

                .card-detail {
                    margin: 0 0 10px;
                    display: grid;
                    gap: 4px;
                }

                .card-detail strong,
                .details-row strong {
                    color: var(--ink);
                }

                .rating-label {
                    display: block;
                    margin-top: 16px;
                    margin-bottom: 8px;
                    font-size: 0.86rem;
                    font-weight: 800;
                    letter-spacing: 0.04em;
                    text-transform: uppercase;
                    color: var(--accent);
                }

                select {
                    width: 100%;
                    border: 1px solid rgba(26, 34, 51, 0.16);
                    border-radius: 14px;
                    padding: 12px 14px;
                    font: inherit;
                    color: var(--ink);
                    background: #ffffff;
                }

                .form-actions {
                    padding: 20px 24px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 16px;
                }

                .primary-button,
                .secondary-link {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 999px;
                    font-family: "Space Grotesk", "Segoe UI", sans-serif;
                    font-weight: 700;
                    text-decoration: none;
                    transition: transform 160ms ease, background-color 160ms ease, color 160ms ease;
                }

                .primary-button {
                    border: 0;
                    padding: 14px 20px;
                    background: linear-gradient(135deg, #16324f, #0f766e);
                    color: #ffffff;
                    cursor: pointer;
                }

                .primary-button:hover,
                .secondary-link:hover {
                    transform: translateY(-1px);
                }

                .secondary-link {
                    padding: 12px 18px;
                    border: 1px solid rgba(15, 118, 110, 0.24);
                    background: var(--accent-soft);
                    color: var(--accent);
                }

                .back-link {
                    margin: 22px 4px 0;
                }

                .table-shell {
                    overflow-x: auto;
                    border-radius: 28px;
                    background: var(--panel-strong);
                    backdrop-filter: blur(10px);
                }

                table {
                    width: 100%;
                    min-width: 900px;
                    border-collapse: separate;
                    border-spacing: 0;
                }

                th,
                td {
                    padding: 18px 16px;
                    text-align: left;
                    vertical-align: top;
                    border-bottom: 1px solid var(--line);
                }

                th {
                    position: sticky;
                    top: 0;
                    z-index: 1;
                    background: linear-gradient(180deg, #1d3148 0%, #142235 100%);
                    color: #fdfbf7;
                    font-size: 0.84rem;
                    letter-spacing: 0.06em;
                    text-transform: uppercase;
                }

                tbody tr {
                    background: transparent;
                    transition: background-color 180ms ease;
                }

                tbody tr:nth-child(even) {
                    background: rgba(26, 34, 51, 0.028);
                }

                tbody tr:hover {
                    background: rgba(15, 118, 110, 0.08);
                }

                tbody tr:last-child td {
                    border-bottom: 0;
                }

                .rank-cell,
                .poster-cell,
                .score-cell {
                    white-space: nowrap;
                }

                .rank-badge {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    width: 38px;
                    height: 38px;
                    border-radius: 50%;
                    background: linear-gradient(135deg, #16324f, #0f766e);
                    color: #ffffff;
                    font-weight: 800;
                    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
                }

                img {
                    width: 100%;
                    max-width: 185px;
                    aspect-ratio: 185 / 278;
                    object-fit: cover;
                    border-radius: 18px;
                    border: 1px solid rgba(26, 34, 51, 0.08);
                    box-shadow: 0 14px 30px rgba(28, 37, 54, 0.18);
                    background: #ebe6de;
                }

                .poster-cell img {
                    width: 78px;
                    height: 114px;
                }

                .movie-link {
                    color: var(--ink);
                    text-decoration: none;
                    font-family: "Space Grotesk", "Segoe UI", sans-serif;
                    font-size: 1.08rem;
                    font-weight: 700;
                    line-height: 1.35;
                }

                .movie-link:hover {
                    color: var(--accent);
                }

                .meta {
                    display: inline-flex;
                    align-items: center;
                    margin-top: 10px;
                    padding: 5px 11px;
                    border-radius: 999px;
                    background: var(--accent-soft);
                    color: var(--accent);
                    font-size: 0.82rem;
                    font-weight: 800;
                    letter-spacing: 0.04em;
                }

                .details-cell {
                    color: var(--muted);
                }

                .details-row + .details-row {
                    margin-top: 8px;
                }

                .details-row strong {
                    display: inline-block;
                    min-width: 84px;
                    margin-right: 10px;
                }

                .score-pill {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    padding: 10px 14px;
                    border-radius: 999px;
                    background: var(--warm-soft);
                    color: var(--warm);
                    border: 1px solid rgba(197, 127, 47, 0.22);
                    font-family: "Space Grotesk", "Segoe UI", sans-serif;
                    font-weight: 700;
                    letter-spacing: 0.03em;
                }

                .note {
                    margin: 0;
                    font-size: 0.96rem;
                }

                .state-card {
                    padding: 28px;
                    margin-bottom: 24px;
                }

                .state-card h2 {
                    font-size: 1.8rem;
                }

                .state-card p {
                    margin: 12px 0 0;
                }

                @media (max-width: 720px) {
                    .page {
                        padding: 28px 14px 40px;
                    }

                    .hero,
                    .state-card,
                    .form-actions {
                        padding: 24px 20px;
                        border-radius: 22px;
                    }

                    .table-shell,
                    .rating-card {
                        border-radius: 22px;
                    }

                    .form-actions {
                        align-items: stretch;
                        flex-direction: column;
                    }

                    .primary-button,
                    .secondary-link {
                        width: 100%;
                    }

                    th,
                    td {
                        padding: 14px 12px;
                    }

                    .poster-cell img {
                        width: 68px;
                        height: 100px;
                        border-radius: 14px;
                    }
                }
                </style>
                """;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }

    public static void main(String[] args) {
        configureUtf8Stdout();
        RecommendationRunner runner = new RecommendationRunner();
        String sampleRaterId = args.length > 0 ? args[0] : "65";
        System.out.print(runner.renderRecommendationsPage(sampleRaterId));
    }

    private static void configureUtf8Stdout() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8.name()));
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is not supported", exception);
        }
    }
}
