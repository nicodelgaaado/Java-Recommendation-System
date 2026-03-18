import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MovieDatabase {
    private static Map<String, Movie> ourMovies;
    private static String ourSource;

    public static void initialize(String movieFile) {
        if (ourMovies != null && normalize(movieFile).equals(ourSource)) {
            return;
        }

        FirstRatings firstRatings = new FirstRatings();
        ArrayList<Movie> movies = firstRatings.loadMovies(movieFile);
        LinkedHashMap<String, Movie> loadedMovies = new LinkedHashMap<>();
        for (Movie movie : movies) {
            loadedMovies.put(movie.getID(), movie);
        }

        ourMovies = loadedMovies;
        ourSource = normalize(movieFile);
    }

    private static void initialize() {
        if (ourMovies == null) {
            initialize("ratedmoviesfull.csv");
        }
    }

    public static void reset() {
        ourMovies = null;
        ourSource = null;
    }

    public static boolean containsID(String id) {
        initialize();
        return ourMovies.containsKey(id);
    }

    public static int getYear(String id) {
        initialize();
        return getMovieOrThrow(id).getYear();
    }

    public static String getGenres(String id) {
        initialize();
        return getMovieOrThrow(id).getGenres();
    }

    public static String getTitle(String id) {
        initialize();
        return getMovieOrThrow(id).getTitle();
    }

    public static Movie getMovie(String id) {
        initialize();
        return ourMovies.get(id);
    }

    public static String getPoster(String id) {
        initialize();
        return getMovieOrThrow(id).getPoster();
    }

    public static int getMinutes(String id) {
        initialize();
        return getMovieOrThrow(id).getMinutes();
    }

    public static String getCountry(String id) {
        initialize();
        return getMovieOrThrow(id).getCountry();
    }

    public static String getDirector(String id) {
        initialize();
        return getMovieOrThrow(id).getDirector();
    }

    public static int size() {
        initialize();
        return ourMovies.size();
    }

    public static ArrayList<String> filterBy(Filter filter) {
        initialize();
        ArrayList<String> matches = new ArrayList<>();
        for (String id : ourMovies.keySet()) {
            if (filter.satisfies(id)) {
                matches.add(id);
            }
        }
        return matches;
    }

    private static Movie getMovieOrThrow(String id) {
        Movie movie = ourMovies.get(id);
        if (movie == null) {
            throw new IllegalArgumentException("Unknown movie id: " + id);
        }
        return movie;
    }

    private static String normalize(String movieFile) {
        return movieFile.replace('\\', '/');
    }
}
