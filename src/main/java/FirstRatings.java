import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FirstRatings {
    public ArrayList<Movie> loadMovies(String fileName) {
        ArrayList<Movie> movies = new ArrayList<>();
        List<Map<String, String>> rows = CsvUtils.readRows(fileName);
        for (Map<String, String> row : rows) {
            Movie movie = new Movie(
                    row.get("id"),
                    row.get("title"),
                    row.get("year"),
                    row.get("genre"),
                    row.get("director"),
                    row.get("country"),
                    row.get("poster"),
                    Integer.parseInt(row.get("minutes"))
            );
            movies.add(movie);
        }
        return movies;
    }

    public void testLoadMovies() {
        ArrayList<Movie> movies = loadMovies("ratedmovies_short.csv");
        System.out.println("Total movies: " + movies.size());

        int comedies = 0;
        int longMovies = 0;
        Map<String, Integer> movieCountByDirector = new LinkedHashMap<>();

        for (Movie movie : movies) {
            if (movie.getGenres().contains("Comedy")) {
                comedies++;
            }
            if (movie.getMinutes() > 150) {
                longMovies++;
            }

            for (String director : splitValues(movie.getDirector())) {
                movieCountByDirector.merge(director, 1, Integer::sum);
            }
        }

        int maxDirectedMovies = 0;
        ArrayList<String> busiestDirectors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : movieCountByDirector.entrySet()) {
            int movieCount = entry.getValue();
            if (movieCount > maxDirectedMovies) {
                maxDirectedMovies = movieCount;
                busiestDirectors.clear();
                busiestDirectors.add(entry.getKey());
            } else if (movieCount == maxDirectedMovies) {
                busiestDirectors.add(entry.getKey());
            }
        }

        System.out.println("Comedy movies: " + comedies);
        System.out.println("Movies longer than 150 minutes: " + longMovies);
        System.out.println("Maximum movies by one director: " + maxDirectedMovies);
        System.out.println("Directors with that many movies: " + busiestDirectors);
    }

    public ArrayList<Rater> loadRaters(String fileName) {
        Map<String, Rater> ratersById = new LinkedHashMap<>();
        List<Map<String, String>> rows = CsvUtils.readRows(fileName);
        for (Map<String, String> row : rows) {
            String raterId = row.get("rater_id");
            String movieId = row.get("movie_id");
            double rating = Double.parseDouble(row.get("rating"));

            Rater rater = ratersById.computeIfAbsent(raterId, EfficientRater::new);
            rater.addRating(movieId, rating);
        }
        return new ArrayList<>(ratersById.values());
    }

    public void testLoadRaters() {
        ArrayList<Rater> raters = loadRaters("ratings_short.csv");
        System.out.println("Total raters: " + raters.size());

        for (Rater rater : raters) {
            System.out.println("Rater " + rater.getID() + " has " + rater.numRatings() + " ratings");
            for (String movieId : rater.getItemsRated()) {
                System.out.println("  " + movieId + " -> " + rater.getRating(movieId));
            }
        }

        String targetRater = "2";
        for (Rater rater : raters) {
            if (rater.getID().equals(targetRater)) {
                System.out.println("Ratings for rater " + targetRater + ": " + rater.numRatings());
            }
        }

        int maxRatings = 0;
        ArrayList<String> busiestRaters = new ArrayList<>();
        for (Rater rater : raters) {
            int count = rater.numRatings();
            if (count > maxRatings) {
                maxRatings = count;
                busiestRaters.clear();
                busiestRaters.add(rater.getID());
            } else if (count == maxRatings) {
                busiestRaters.add(rater.getID());
            }
        }

        System.out.println("Maximum ratings by any rater: " + maxRatings);
        System.out.println("Raters with that many ratings: " + busiestRaters);

        String targetMovie = "1798709";
        int ratingsForMovie = 0;
        ArrayList<String> distinctMovies = new ArrayList<>();
        for (Rater rater : raters) {
            for (String movieId : rater.getItemsRated()) {
                if (movieId.equals(targetMovie)) {
                    ratingsForMovie++;
                }
                if (!distinctMovies.contains(movieId)) {
                    distinctMovies.add(movieId);
                }
            }
        }

        System.out.println("Ratings for movie " + targetMovie + ": " + ratingsForMovie);
        System.out.println("Different movies rated: " + distinctMovies.size());
    }

    private ArrayList<String> splitValues(String rawValue) {
        ArrayList<String> values = new ArrayList<>();
        for (String value : rawValue.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    public static void main(String[] args) {
        FirstRatings firstRatings = new FirstRatings();
        firstRatings.testLoadMovies();
        System.out.println();
        firstRatings.testLoadRaters();
    }
}
