import java.util.ArrayList;

public class ThirdRatings {
    private final ArrayList<Rater> myRaters;

    public ThirdRatings() {
        this("ratings.csv");
    }

    public ThirdRatings(String ratingsFile) {
        FirstRatings firstRatings = new FirstRatings();
        myRaters = firstRatings.loadRaters(ratingsFile);
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
        return getAverageRatingsByFilter(minimalRaters, new TrueFilter());
    }

    public ArrayList<Rating> getAverageRatingsByFilter(int minimalRaters, Filter filterCriteria) {
        ArrayList<Rating> averages = new ArrayList<>();
        for (String movieId : MovieDatabase.filterBy(filterCriteria)) {
            double average = getAverageByID(movieId, minimalRaters);
            if (average > 0.0) {
                averages.add(new Rating(movieId, average));
            }
        }
        return averages;
    }
}
