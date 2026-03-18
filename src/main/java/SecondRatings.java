import java.util.ArrayList;

public class SecondRatings {
    private final ArrayList<Movie> myMovies;
    private final ArrayList<Rater> myRaters;

    public SecondRatings() {
        this("ratedmoviesfull.csv", "ratings.csv");
    }

    public SecondRatings(String movieFile, String ratingFile) {
        FirstRatings firstRatings = new FirstRatings();
        myMovies = firstRatings.loadMovies(movieFile);
        myRaters = firstRatings.loadRaters(ratingFile);
    }

    public int getMovieSize() {
        return myMovies.size();
    }

    public int getRaterSize() {
        return myRaters.size();
    }

    private double getAverageByID(String id, int minimalRaters) {
        int ratingsCount = 0;
        double total = 0.0;

        for (Rater rater : myRaters) {
            double rating = rater.getRating(id);
            if (rating != -1) {
                total += rating;
                ratingsCount++;
            }
        }

        if (ratingsCount < minimalRaters) {
            return 0.0;
        }
        return total / ratingsCount;
    }

    public ArrayList<Rating> getAverageRatings(int minimalRaters) {
        ArrayList<Rating> averages = new ArrayList<>();
        for (Movie movie : myMovies) {
            double average = getAverageByID(movie.getID(), minimalRaters);
            if (average > 0.0) {
                averages.add(new Rating(movie.getID(), average));
            }
        }
        return averages;
    }

    public String getTitle(String id) {
        for (Movie movie : myMovies) {
            if (movie.getID().equals(id)) {
                return movie.getTitle();
            }
        }
        return "ID was not found";
    }

    public String getID(String title) {
        for (Movie movie : myMovies) {
            if (movie.getTitle().equals(title)) {
                return movie.getID();
            }
        }
        return "NO SUCH TITLE.";
    }
}
