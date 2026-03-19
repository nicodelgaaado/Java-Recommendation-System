import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RecommendationRunner implements Recommender {
    private static final String MOVIE_FILE = "ratedmoviesfull.csv";
    private static final String RATING_FILE = "ratings.csv";
    private static final int ITEMS_TO_RATE = 12;
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
        initializeDatabases();
        printStyles();
        printPageStart();

        Rater webRater = RaterDatabase.getRater(webRaterID);
        if (webRater == null) {
            printStateCard("Ratings not found", "Unable to find ratings for user " + escapeHtml(webRaterID) + ".");
            printPageEnd();
            return;
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
            printStateCard("No recommendations yet", "Try rating a few more movies to generate stronger matches.");
            printPageEnd();
            return;
        }

        System.out.println("<section class=\"hero\">");
        System.out.println("<p class=\"eyebrow\">Personalized discovery</p>");
        System.out.println("<h2>Recommended Movies</h2>");
        System.out.println("<p class=\"intro\">Weighted picks based on viewers whose rating patterns are closest to yours. The original ranking layout stays intact, with a cleaner presentation and better mobile behavior.</p>");
        System.out.println("</section>");
        System.out.println("<div class=\"table-shell\">");
        System.out.println("<table>");
        System.out.println("<thead><tr><th>#</th><th>Poster</th><th>Movie</th><th>Details</th><th>Score</th></tr></thead>");
        System.out.println("<tbody>");

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
            System.out.println("<tr>");
            System.out.println("<td class=\"rank-cell\"><span class=\"rank-badge\">" + (index + 1) + "</span></td>");
            System.out.println("<td class=\"poster-cell\"><img src=\"" + escapeHtml(posterUrl) + "\" onerror=\"this.onerror=null;this.src='" + escapeHtmlAttribute(fallbackPosterUrl) + "';\" alt=\"" + escapeHtml(movie.getTitle()) + " poster\"></td>");
            System.out.println("<td class=\"movie-cell\"><a class=\"movie-link\" href=\"" + imdbLink + "\">" + escapeHtml(movie.getTitle()) + "</a><br><span class=\"meta\">" + movie.getYear() + "</span></td>");
            System.out.println("<td class=\"details-cell\">"
                    + "<div class=\"details-row\"><strong>Genres</strong><span>" + escapeHtml(movie.getGenres()) + "</span></div>"
                    + "<div class=\"details-row\"><strong>Directors</strong><span>" + escapeHtml(movie.getDirector()) + "</span></div>"
                    + "<div class=\"details-row\"><strong>Country</strong><span>" + escapeHtml(movie.getCountry()) + "</span></div>"
                    + "<div class=\"details-row\"><strong>Minutes</strong><span>" + movie.getMinutes() + "</span></div>"
                    + "</td>");
            System.out.println("<td class=\"score-cell\"><span class=\"score-pill\">" + String.format(Locale.US, "%.2f", recommendation.getValue()) + "</span></td>");
            System.out.println("</tr>");
        }

        System.out.println("</tbody>");
        System.out.println("</table>");
        System.out.println("</div>");
        System.out.println("<p class=\"note\">Scores are weighted averages from users with the most similar rating patterns.</p>");
        printPageEnd();
    }

    private void initializeDatabases() {
        MovieDatabase.initialize(MOVIE_FILE);
        RaterDatabase.initialize(RATING_FILE);
    }

    private void printStyles() {
        System.out.println("""
                <style>
                :root {
                    --bg: #f4efe8;
                    --panel: rgba(255, 252, 247, 0.92);
                    --panel-strong: #fffdf9;
                    --ink: #1a2233;
                    --muted: #5f6777;
                    --line: rgba(26, 34, 51, 0.12);
                    --line-strong: rgba(26, 34, 51, 0.2);
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
                    background: var(--bg);
                }

                .page {
                    max-width: 1180px;
                    margin: 0 auto;
                    padding: 48px 20px 64px;
                }

                .hero,
                .table-shell,
                .state-card {
                    border: 1px solid var(--line);
                    box-shadow: var(--shadow);
                }

                .hero,
                .state-card {
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

                .intro,
                .note,
                .state-card p {
                    color: var(--muted);
                    line-height: 1.7;
                }

                .intro {
                    max-width: 760px;
                    margin: 14px 0 0;
                    font-size: 1rem;
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
                    width: 78px;
                    height: 114px;
                    object-fit: cover;
                    border-radius: 18px;
                    border: 1px solid rgba(26, 34, 51, 0.08);
                    box-shadow: 0 14px 30px rgba(28, 37, 54, 0.18);
                    background: #ebe6de;
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
                    color: var(--ink);
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
                    margin: 18px 4px 0;
                    font-size: 0.96rem;
                }

                .state-card {
                    padding: 28px;
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
                    .state-card {
                        padding: 24px 20px;
                        border-radius: 22px;
                    }

                    .table-shell {
                        border-radius: 22px;
                    }

                    th,
                    td {
                        padding: 14px 12px;
                    }

                    img {
                        width: 68px;
                        height: 100px;
                        border-radius: 14px;
                    }
                }
                </style>
                """);
    }

    private void printPageStart() {
        System.out.println("<main class=\"page\">");
    }

    private void printPageEnd() {
        System.out.println("</main>");
    }

    private void printStateCard(String title, String message) {
        System.out.println("<section class=\"state-card\">");
        System.out.println("<p class=\"eyebrow\">Personalized discovery</p>");
        System.out.println("<h2>" + title + "</h2>");
        System.out.println("<p>" + message + "</p>");
        System.out.println("</section>");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void main(String[] args) {
        configureUtf8Stdout();
        RecommendationRunner runner = new RecommendationRunner();
        String sampleRaterId = args.length > 0 ? args[0] : "65";
        System.out.println("<!DOCTYPE html>");
        System.out.println("<html lang=\"en\">");
        System.out.println("<head>");
        System.out.println("<meta charset=\"utf-8\">");
        System.out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        System.out.println("<title>Recommended Movies</title>");
        System.out.println("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">");
        System.out.println("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>");
        System.out.println("<link href=\"https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Space+Grotesk:wght@500;700&display=swap\" rel=\"stylesheet\">");
        System.out.println("</head>");
        System.out.println("<body>");
        System.out.println("<!-- Movies to rate: " + runner.getItemsToRate() + " -->");
        runner.printRecommendationsFor(sampleRaterId);
        System.out.println("</body>");
        System.out.println("</html>");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }

    private static void configureUtf8Stdout() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }
}
