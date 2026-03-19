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

    @Override
    public ArrayList<String> getItemsToRate() {
        initializeDatabases();

        ArrayList<String> items = new ArrayList<String>();
        Set<String> primaryDirectors = new HashSet<String>();
        FourthRatings fourthRatings = new FourthRatings();

        collectSeedMovies(items, primaryDirectors, fourthRatings.getAverageRatings(50), true);
        collectSeedMovies(items, primaryDirectors, fourthRatings.getAverageRatings(20), false);
        collectSeedMovies(items, primaryDirectors, fourthRatings.getAverageRatings(5), false);

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
        html.append("<h1>Rate Movies</h1>");
        html.append("<p class=\"intro\">Rate at least ");
        html.append(MINIMUM_SUBMITTED_RATINGS);
        html.append(" movies on the 0 to 10 scale. The recommender will compare your ratings with similar viewers and show a table of recommendations.</p>");
        html.append("</section>");

        if (!isBlank(errorMessage)) {
            html.append(buildStateCard("More ratings needed", errorMessage));
        }

        html.append("<form class=\"rating-form\" method=\"post\" action=\"");
        html.append(escapeHtml(formAction));
        html.append("\">");
        html.append("<table class=\"rating-table\">");
        html.append("<thead><tr><th>#</th><th>Movie</th><th>Year</th><th>Genres</th><th>Director</th><th>Minutes</th><th>Your Rating</th></tr></thead>");
        html.append("<tbody>");

        ArrayList<String> itemsToRate = getItemsToRate();
        for (int index = 0; index < itemsToRate.size(); index++) {
            String movieId = itemsToRate.get(index);
            Movie movie = MovieDatabase.getMovie(movieId);
            if (movie == null) {
                continue;
            }

            Integer selectedRating = selectedRatings == null ? null : selectedRatings.get(movieId);
            html.append("<tr>");
            html.append("<td>").append(index + 1).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getTitle())).append("</td>");
            html.append("<td>").append(movie.getYear()).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getGenres())).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getDirector())).append("</td>");
            html.append("<td>").append(movie.getMinutes()).append("</td>");
            html.append("<td><select name=\"rating-").append(escapeHtmlAttribute(movieId)).append("\">");
            html.append("<option value=\"\">Not rated</option>");
            for (int value = 10; value >= 0; value--) {
                html.append("<option value=\"").append(value).append("\"");
                if (selectedRating != null && selectedRating.intValue() == value) {
                    html.append(" selected");
                }
                html.append(">").append(value).append(" / 10</option>");
            }
            html.append("</select></td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");
        html.append("<div class=\"form-actions\">");
        html.append("<p class=\"note\">Movies are selected from the existing data set. Blank entries are ignored.</p>");
        html.append("<button class=\"primary-button\" type=\"submit\">Get recommendations</button>");
        html.append("</div>");
        html.append("</form>");
        html.append("</main>");
        html.append(documentEnd());
        return html.toString();
    }

    public String renderRecommendationsPage(String webRaterID) {
        return documentStart("Recommended Movies") + renderRecommendationsContent(webRaterID) + documentEnd();
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
            html.append(buildStateCard("Ratings not found", "Unable to find ratings for user " + escapeHtml(webRaterID) + "."));
            html.append("</main>");
            return html.toString();
        }

        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> rawRecommendations = fourthRatings.getSimilarRatings(webRaterID, SIMILAR_RATERS, MINIMAL_RATERS);
        ArrayList<Rating> recommendations = new ArrayList<Rating>();
        Set<String> ratedMovieIds = new HashSet<String>(webRater.getItemsRated());

        for (Rating recommendation : rawRecommendations) {
            if (!ratedMovieIds.contains(recommendation.getItem())) {
                recommendations.add(recommendation);
            }
            if (recommendations.size() == MAX_RECOMMENDATIONS) {
                break;
            }
        }

        if (recommendations.isEmpty()) {
            html.append(buildStateCard("No recommendations yet", "Try rating a few more movies to generate stronger matches."));
            html.append("<p class=\"back-link\"><a class=\"secondary-link\" href=\"/\">Rate another set of movies</a></p>");
            html.append("</main>");
            return html.toString();
        }

        html.append("<section class=\"hero\">");
        html.append("<h1>Recommended Movies</h1>");
        html.append("<p class=\"intro\">These recommendations are weighted by viewers whose rating patterns are closest to yours. Movies you already rated are excluded.</p>");
        html.append("</section>");
        html.append("<table>");
        html.append("<thead><tr><th>#</th><th>Movie</th><th>Year</th><th>Genres</th><th>Director</th><th>Country</th><th>Minutes</th><th>Score</th></tr></thead>");
        html.append("<tbody>");

        for (int index = 0; index < recommendations.size(); index++) {
            Rating recommendation = recommendations.get(index);
            Movie movie = MovieDatabase.getMovie(recommendation.getItem());
            if (movie == null) {
                continue;
            }

            html.append("<tr>");
            html.append("<td>").append(index + 1).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getTitle())).append("</td>");
            html.append("<td>").append(movie.getYear()).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getGenres())).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getDirector())).append("</td>");
            html.append("<td>").append(escapeHtml(movie.getCountry())).append("</td>");
            html.append("<td>").append(movie.getMinutes()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", recommendation.getValue())).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");
        html.append("<p class=\"note\">Top ");
        html.append(recommendations.size());
        html.append(" recommendations shown.</p>");
        html.append("<p class=\"back-link\"><a class=\"secondary-link\" href=\"/\">Rate another set of movies</a></p>");
        html.append("</main>");
        return html.toString();
    }

    private void collectSeedMovies(ArrayList<String> items, Set<String> primaryDirectors, ArrayList<Rating> candidates, boolean applySeedFilters) {
        Collections.sort(candidates, Collections.reverseOrder());
        for (Rating rating : candidates) {
            if (items.size() == ITEMS_TO_RATE) {
                return;
            }

            Movie movie = MovieDatabase.getMovie(rating.getItem());
            if (movie == null) {
                continue;
            }

            if (applySeedFilters && !isGoodSeedMovie(movie)) {
                continue;
            }

            if (items.contains(movie.getID())) {
                continue;
            }

            String primaryDirector = getPrimaryDirector(movie);
            if (items.size() < 6 || primaryDirectors.add(primaryDirector)) {
                items.add(movie.getID());
            }
        }
    }

    private boolean isGoodSeedMovie(Movie movie) {
        return movie.getYear() >= 1980 && movie.getMinutes() >= 80 && movie.getMinutes() <= 180;
    }

    private String getPrimaryDirector(Movie movie) {
        String director = movie.getDirector();
        if (director == null) {
            return "";
        }

        int separator = director.indexOf(',');
        if (separator >= 0) {
            director = director.substring(0, separator);
        }
        return director.trim();
    }

    private void initializeDatabases() {
        MovieDatabase.initialize(MOVIE_FILE);
        RaterDatabase.initialize(RATING_FILE);
    }

    private String buildStateCard(String title, String message) {
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"state-card\">");
        html.append("<h1>").append(escapeHtml(title)).append("</h1>");
        html.append("<p>").append(escapeHtml(message)).append("</p>");
        html.append("</section>");
        return html.toString();
    }

    private String documentStart(String title) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("<meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>").append(escapeHtml(title)).append("</title>");
        html.append("</head>");
        html.append("<body>");
        return html.toString();
    }

    private String documentEnd() {
        return "</body></html>";
    }

    private String buildStyles() {
        return "<style>"
                + "body{margin:0;font-family:Arial,sans-serif;background:#f7f3ec;color:#1f2933;}"
                + ".page{max-width:1100px;margin:0 auto;padding:32px 16px 48px;}"
                + ".hero,.state-card,.form-actions{background:#fffdf8;border:1px solid #d7d2c8;border-radius:16px;box-shadow:0 12px 32px rgba(31,41,51,.08);}"
                + ".hero,.state-card{padding:24px;margin-bottom:20px;}"
                + "h1{margin:0 0 10px;font-size:2rem;line-height:1.1;}"
                + ".intro,.note,.state-card p{margin:0;color:#52606d;line-height:1.6;}"
                + "table,.rating-table{width:100%;border-collapse:collapse;background:#fffdf8;border:1px solid #d7d2c8;box-shadow:0 12px 32px rgba(31,41,51,.08);}"
                + "th,td{padding:12px 10px;border-bottom:1px solid #e4dfd7;text-align:left;vertical-align:top;}"
                + "th{background:#1f3b57;color:#fff;position:sticky;top:0;}"
                + "tr:nth-child(even){background:#faf7f1;}"
                + "select{width:100%;padding:8px;border:1px solid #c9c1b6;border-radius:8px;background:#fff;}"
                + ".rating-form{display:block;}"
                + ".form-actions{display:flex;justify-content:space-between;align-items:center;gap:16px;padding:16px 20px;margin-top:20px;}"
                + ".primary-button,.secondary-link{display:inline-block;padding:12px 18px;border-radius:999px;text-decoration:none;font-weight:bold;}"
                + ".primary-button{border:0;background:#0f766e;color:#fff;cursor:pointer;}"
                + ".secondary-link{background:#e0f2ef;color:#0f766e;border:1px solid #a7d8d1;}"
                + ".back-link{margin-top:18px;}"
                + "@media (max-width:720px){.page{padding:20px 12px 32px;}th,td{padding:10px 8px;font-size:.92rem;}.form-actions{flex-direction:column;align-items:stretch;}}"
                + "</style>";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
