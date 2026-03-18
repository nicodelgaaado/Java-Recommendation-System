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

        Rater webRater = RaterDatabase.getRater(webRaterID);
        if (webRater == null) {
            System.out.println("<p>Unable to find ratings for user " + escapeHtml(webRaterID) + ".</p>");
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
            System.out.println("<p>No recommendations are available yet. Try rating a few more movies.</p>");
            return;
        }

        printStyles();
        System.out.println("<h2>Recommended Movies</h2>");
        System.out.println("<table>");
        System.out.println("<tr><th>#</th><th>Poster</th><th>Movie</th><th>Details</th><th>Score</th></tr>");

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
            System.out.println("<td>" + (index + 1) + "</td>");
            System.out.println("<td><img src=\"" + escapeHtml(posterUrl) + "\" onerror=\"this.onerror=null;this.src='" + escapeHtmlAttribute(fallbackPosterUrl) + "';\" alt=\"" + escapeHtml(movie.getTitle()) + " poster\"></td>");
            System.out.println("<td><a href=\"" + imdbLink + "\">" + escapeHtml(movie.getTitle()) + "</a><br><span class=\"meta\">" + movie.getYear() + "</span></td>");
            System.out.println("<td>"
                    + "<strong>Genres:</strong> " + escapeHtml(movie.getGenres()) + "<br>"
                    + "<strong>Directors:</strong> " + escapeHtml(movie.getDirector()) + "<br>"
                    + "<strong>Country:</strong> " + escapeHtml(movie.getCountry()) + "<br>"
                    + "<strong>Minutes:</strong> " + movie.getMinutes()
                    + "</td>");
            System.out.println("<td>" + String.format(Locale.US, "%.2f", recommendation.getValue()) + "</td>");
            System.out.println("</tr>");
        }

        System.out.println("</table>");
        System.out.println("<p class=\"note\">Scores are weighted averages from users with the most similar rating patterns.</p>");
    }

    private void initializeDatabases() {
        MovieDatabase.initialize(MOVIE_FILE);
        RaterDatabase.initialize(RATING_FILE);
    }

    private void printStyles() {
        System.out.println("<style>");
        System.out.println("body { font-family: Arial, sans-serif; color: #17202a; }");
        System.out.println("h2 { color: #0b3954; }");
        System.out.println("table { border-collapse: collapse; width: 100%; max-width: 1100px; }");
        System.out.println("th, td { border: 1px solid #d5d8dc; padding: 12px; text-align: left; vertical-align: top; }");
        System.out.println("th { background: #0b3954; color: white; }");
        System.out.println("tr:nth-child(even) { background: #f8f9f9; }");
        System.out.println("img { width: 70px; height: 100px; object-fit: cover; border-radius: 4px; }");
        System.out.println("a { color: #117a65; text-decoration: none; font-weight: 600; }");
        System.out.println(".meta { color: #566573; }");
        System.out.println(".note { margin-top: 16px; color: #566573; }");
        System.out.println("</style>");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void main(String[] args) {
        RecommendationRunner runner = new RecommendationRunner();
        String sampleRaterId = args.length > 0 ? args[0] : "65";
        System.out.println("<!DOCTYPE html>");
        System.out.println("<html lang=\"en\">");
        System.out.println("<head><meta charset=\"utf-8\"><title>Recommended Movies</title></head>");
        System.out.println("<body>");
        System.out.println("<!-- Movies to rate: " + runner.getItemsToRate() + " -->");
        runner.printRecommendationsFor(sampleRaterId);
        System.out.println("</body>");
        System.out.println("</html>");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }
}
