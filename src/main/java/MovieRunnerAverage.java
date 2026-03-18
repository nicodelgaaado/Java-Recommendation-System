import java.util.ArrayList;
import java.util.Collections;

public class MovieRunnerAverage {
    private static final String MOVIE_FILE = "ratedmovies_short.csv";
    private static final String RATING_FILE = "ratings_short.csv";

    public void printAverageRatings() {
        SecondRatings secondRatings = new SecondRatings(MOVIE_FILE, RATING_FILE);
        System.out.println("Movies: " + secondRatings.getMovieSize());
        System.out.println("Raters: " + secondRatings.getRaterSize());

        ArrayList<Rating> ratings = secondRatings.getAverageRatings(3);
        Collections.sort(ratings);
        for (Rating rating : ratings) {
            System.out.println(rating.getValue() + " " + secondRatings.getTitle(rating.getItem()));
        }
    }

    public void getAverageRatingOneMovie() {
        SecondRatings secondRatings = new SecondRatings(MOVIE_FILE, RATING_FILE);
        String title = "The Godfather";
        String movieId = secondRatings.getID(title);
        if ("NO SUCH TITLE.".equals(movieId)) {
            System.out.println(movieId);
            return;
        }

        for (Rating rating : secondRatings.getAverageRatings(1)) {
            if (rating.getItem().equals(movieId)) {
                System.out.println(rating.getValue() + " " + title);
                return;
            }
        }

        System.out.println("No average available for " + title);
    }

    public static void main(String[] args) {
        MovieRunnerAverage runner = new MovieRunnerAverage();
        runner.printAverageRatings();
        System.out.println();
        runner.getAverageRatingOneMovie();
    }
}
