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
        initializeDatabases();

        Rater webRater = RaterDatabase.getRater(webRaterID);
        if (webRater == null) {
            System.out.println("<p>Ratings not found for user " + escapeHtml(webRaterID) + ".</p>");
            return;
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

        System.out.println("<style>"
                + "body{font-family:Arial,sans-serif;color:#1f2933;}"
                + "table{border-collapse:collapse;width:100%;max-width:1100px;}"
                + "th,td{border:1px solid #d7d2c8;padding:10px;text-align:left;vertical-align:top;}"
                + "th{background:#1f3b57;color:#fff;}"
                + ".note{margin-top:16px;color:#52606d;}"
                + "</style>");

        if (recommendations.isEmpty()) {
            System.out.println("<p>No recommendations yet. Try rating a few more movies.</p>");
            return;
        }

        System.out.println("<h2>Recommended Movies</h2>");
        System.out.println("<table>");
        System.out.println("<thead><tr><th>#</th><th>Movie</th><th>Year</th><th>Genres</th><th>Director</th><th>Country</th><th>Minutes</th><th>Score</th></tr></thead>");
        System.out.println("<tbody>");

        for (int index = 0; index < recommendations.size(); index++) {
            Rating recommendation = recommendations.get(index);
            Movie movie = MovieDatabase.getMovie(recommendation.getItem());
            if (movie == null) {
                continue;
            }

            System.out.println("<tr>"
                    + "<td>" + (index + 1) + "</td>"
                    + "<td>" + escapeHtml(movie.getTitle()) + "</td>"
                    + "<td>" + movie.getYear() + "</td>"
                    + "<td>" + escapeHtml(movie.getGenres()) + "</td>"
                    + "<td>" + escapeHtml(movie.getDirector()) + "</td>"
                    + "<td>" + escapeHtml(movie.getCountry()) + "</td>"
                    + "<td>" + movie.getMinutes() + "</td>"
                    + "<td>" + String.format(Locale.US, "%.2f", recommendation.getValue()) + "</td>"
                    + "</tr>");
        }

        System.out.println("</tbody>");
        System.out.println("</table>");
        System.out.println("<p class=\"note\">Movies you already rated are excluded from the table.</p>");
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
}
