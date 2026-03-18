import java.util.ArrayList;

public class MovieRunnerSimilarRatings {
    private static final String MOVIE_FILE = "ratedmoviesfull.csv";
    private static final String RATING_FILE = "ratings.csv";

    public void printAverageRatings() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> ratings = fourthRatings.getAverageRatings(35);
        System.out.println("found " + ratings.size() + " movies");
        for (int index = 0; index < Math.min(10, ratings.size()); index++) {
            Rating rating = ratings.get(index);
            System.out.println(rating.getValue() + " " + MovieDatabase.getTitle(rating.getItem()));
        }
    }

    public void printAverageRatingsByYearAfterAndGenre() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new YearAfterFilter(1990));
        filters.addFilter(new GenreFilter("Drama"));
        ArrayList<Rating> ratings = fourthRatings.getAverageRatingsByFilter(8, filters);
        System.out.println("found " + ratings.size() + " movies");
        for (int index = 0; index < Math.min(10, ratings.size()); index++) {
            Rating rating = ratings.get(index);
            System.out.println(rating.getValue() + " " + MovieDatabase.getTitle(rating.getItem()));
        }
    }

    public void printSimilarRatings() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> ratings = fourthRatings.getSimilarRatings("65", 20, 5);
        System.out.println("Found ratings for movies: " + ratings.size());
        printMovieRows(ratings, 10, false, false, false, false);
    }

    public void printSimilarRatingsByGenre() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> ratings = fourthRatings.getSimilarRatingsByFilter("65", 20, 5, new GenreFilter("Action"));
        System.out.println("Found ratings for movies: " + ratings.size());
        printMovieRows(ratings, 10, true, false, false, false);
    }

    public void printSimilarRatingsByDirector() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        ArrayList<Rating> ratings = fourthRatings.getSimilarRatingsByFilter(
                "1034",
                10,
                3,
                new DirectorsFilter("Clint Eastwood,Sydney Pollack,David Cronenberg,Oliver Stone")
        );
        System.out.println("Found ratings for movies: " + ratings.size());
        printMovieRows(ratings, 10, false, true, false, false);
    }

    public void printSimilarRatingsByGenreAndMinutes() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new GenreFilter("Adventure"));
        filters.addFilter(new MinutesFilter(100, 200));
        ArrayList<Rating> ratings = fourthRatings.getSimilarRatingsByFilter("65", 10, 5, filters);
        System.out.println("Found ratings for movies: " + ratings.size());
        printMovieRows(ratings, 10, true, false, true, false);
    }

    public void printSimilarRatingsByYearAfterAndMinutes() {
        loadDatabases();
        FourthRatings fourthRatings = new FourthRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new YearAfterFilter(2000));
        filters.addFilter(new MinutesFilter(80, 100));
        ArrayList<Rating> ratings = fourthRatings.getSimilarRatingsByFilter("65", 10, 5, filters);
        System.out.println("Found ratings for movies: " + ratings.size());
        printMovieRows(ratings, 10, false, false, true, true);
    }

    private void loadDatabases() {
        MovieDatabase.initialize(MOVIE_FILE);
        RaterDatabase.initialize(RATING_FILE);
    }

    private void printMovieRows(ArrayList<Rating> ratings, int limit, boolean showGenres, boolean showDirectors,
                                boolean showMinutes, boolean showYear) {
        int max = Math.min(limit, ratings.size());
        for (int index = 0; index < max; index++) {
            Rating rating = ratings.get(index);
            String movieId = rating.getItem();
            StringBuilder line = new StringBuilder();
            line.append(rating.getValue());
            line.append(" ");
            if (showYear) {
                line.append(MovieDatabase.getYear(movieId)).append(" ");
            }
            if (showMinutes) {
                line.append(MovieDatabase.getMinutes(movieId)).append(" min ");
            }
            line.append(MovieDatabase.getTitle(movieId));
            System.out.println(line);

            if (showGenres) {
                System.out.println(MovieDatabase.getGenres(movieId));
            }
            if (showDirectors) {
                System.out.println(MovieDatabase.getDirector(movieId));
            }
        }
    }

    public static void main(String[] args) {
        MovieRunnerSimilarRatings runner = new MovieRunnerSimilarRatings();
        runner.printSimilarRatings();
        System.out.println();
        runner.printSimilarRatingsByGenre();
        System.out.println();
        runner.printSimilarRatingsByDirector();
        System.out.println();
        runner.printSimilarRatingsByGenreAndMinutes();
        System.out.println();
        runner.printSimilarRatingsByYearAfterAndMinutes();
    }
}
