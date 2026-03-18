import java.util.ArrayList;
import java.util.Collections;

public class MovieRunnerWithFilters {
    private static final String MOVIE_FILE = "ratedmovies_short.csv";
    private static final String RATING_FILE = "ratings_short.csv";

    public void printAverageRatings() {
        ThirdRatings thirdRatings = loadThirdRatings();
        ArrayList<Rating> ratings = thirdRatings.getAverageRatings(1);
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println("found " + ratings.size() + " movies");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + MovieDatabase.getTitle(rating.getItem()));
        }
    }

    public void printAverageRatingsByYear() {
        ThirdRatings thirdRatings = loadThirdRatings();
        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(1, new YearAfterFilter(2000));
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println("found " + ratings.size() + " movies");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + MovieDatabase.getYear(rating.getItem()) + " " + MovieDatabase.getTitle(rating.getItem()));
        }
    }

    public void printAverageRatingsByGenre() {
        ThirdRatings thirdRatings = loadThirdRatings();
        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(1, new GenreFilter("Crime"));
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println("found " + ratings.size() + " movies");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + MovieDatabase.getTitle(rating.getItem()));
            System.out.println(MovieDatabase.getGenres(rating.getItem()));
        }
    }

    public void printAverageRatingsByMinutes() {
        ThirdRatings thirdRatings = loadThirdRatings();
        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(1, new MinutesFilter(110, 170));
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println("found " + ratings.size() + " movies");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " Time: " + MovieDatabase.getMinutes(rating.getItem()) + " " + MovieDatabase.getTitle(rating.getItem()));
        }
    }

    public void printAverageRatingsByDirectors() {
        ThirdRatings thirdRatings = loadThirdRatings();
        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(
                1,
                new DirectorsFilter("Charles Chaplin,Michael Mann,Spike Jonze")
        );
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println("found " + ratings.size() + " movies");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + MovieDatabase.getTitle(rating.getItem()));
            System.out.println(MovieDatabase.getDirector(rating.getItem()));
        }
    }

    public void printAverageRatingsByYearAfterAndGenre() {
        ThirdRatings thirdRatings = loadThirdRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new YearAfterFilter(1980));
        filters.addFilter(new GenreFilter("Romance"));

        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(1, filters);
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println(ratings.size() + " movie matched");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + MovieDatabase.getYear(rating.getItem()) + " " + MovieDatabase.getTitle(rating.getItem()));
            System.out.println(MovieDatabase.getGenres(rating.getItem()));
        }
    }

    public void printAverageRatingsByDirectorsAndMinutes() {
        ThirdRatings thirdRatings = loadThirdRatings();
        AllFilters filters = new AllFilters();
        filters.addFilter(new MinutesFilter(30, 170));
        filters.addFilter(new DirectorsFilter("Spike Jonze,Michael Mann,Charles Chaplin,Francis Ford Coppola"));

        ArrayList<Rating> ratings = thirdRatings.getAverageRatingsByFilter(1, filters);
        Collections.sort(ratings);

        System.out.println("read data for " + thirdRatings.getRaterSize() + " raters");
        System.out.println("read data for " + MovieDatabase.size() + " movies");
        System.out.println(ratings.size() + " movies matched");

        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " Time: " + MovieDatabase.getMinutes(rating.getItem()) + " " + MovieDatabase.getTitle(rating.getItem()));
            System.out.println(MovieDatabase.getDirector(rating.getItem()));
        }
    }

    private ThirdRatings loadThirdRatings() {
        MovieDatabase.initialize(MOVIE_FILE);
        return new ThirdRatings(RATING_FILE);
    }

    public static void main(String[] args) {
        MovieRunnerWithFilters runner = new MovieRunnerWithFilters();
        runner.printAverageRatings();
        System.out.println();
        runner.printAverageRatingsByYear();
        System.out.println();
        runner.printAverageRatingsByGenre();
        System.out.println();
        runner.printAverageRatingsByMinutes();
        System.out.println();
        runner.printAverageRatingsByDirectors();
        System.out.println();
        runner.printAverageRatingsByYearAfterAndGenre();
        System.out.println();
        runner.printAverageRatingsByDirectorsAndMinutes();
    }
}
