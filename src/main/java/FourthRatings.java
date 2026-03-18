import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FourthRatings {
    private double getAverageByID(String id, int minimalRaters) {
        int ratingsCount = 0;
        double total = 0.0;

        for (Rater rater : RaterDatabase.getRaters()) {
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

    private double dotProduct(Rater me, Rater other) {
        double total = 0.0;
        for (String movieId : me.getItemsRated()) {
            if (other.hasRating(movieId)) {
                total += (me.getRating(movieId) - 5) * (other.getRating(movieId) - 5);
            }
        }
        return total;
    }

    private ArrayList<Rating> getSimilarities(String id) {
        Rater targetRater = RaterDatabase.getRater(id);
        if (targetRater == null) {
            return new ArrayList<>();
        }

        ArrayList<Rating> similarities = new ArrayList<>();
        for (Rater otherRater : RaterDatabase.getRaters()) {
            if (otherRater.getID().equals(id)) {
                continue;
            }

            double similarity = dotProduct(targetRater, otherRater);
            if (similarity > 0) {
                similarities.add(new Rating(otherRater.getID(), similarity));
            }
        }

        similarities.sort(Collections.reverseOrder());
        return similarities;
    }

    public ArrayList<Rating> getSimilarRatings(String id, int numSimilarRaters, int minimalRaters) {
        return getSimilarRatingsByFilter(id, numSimilarRaters, minimalRaters, new TrueFilter());
    }

    public ArrayList<Rating> getSimilarRatingsByFilter(String id, int numSimilarRaters, int minimalRaters, Filter filterCriteria) {
        ArrayList<Rating> similarities = getSimilarities(id);
        if (similarities.isEmpty()) {
            return new ArrayList<>();
        }

        int limit = Math.min(numSimilarRaters, similarities.size());
        List<Rating> topSimilarities = similarities.subList(0, limit);
        ArrayList<Rating> weightedRatings = new ArrayList<>();

        for (String movieId : MovieDatabase.filterBy(filterCriteria)) {
            double weightedAverage = getWeightedAverage(movieId, topSimilarities, minimalRaters);
            if (weightedAverage > 0.0) {
                weightedRatings.add(new Rating(movieId, weightedAverage));
            }
        }

        weightedRatings.sort(Collections.reverseOrder());
        return weightedRatings;
    }

    private double getWeightedAverage(String movieId, List<Rating> topSimilarities, int minimalRaters) {
        int ratingsCount = 0;
        double total = 0.0;

        for (Rating similarityRating : topSimilarities) {
            Rater similarRater = RaterDatabase.getRater(similarityRating.getItem());
            if (similarRater == null) {
                continue;
            }

            double movieRating = similarRater.getRating(movieId);
            if (movieRating != -1) {
                total += similarityRating.getValue() * movieRating;
                ratingsCount++;
            }
        }

        if (ratingsCount < minimalRaters) {
            return 0.0;
        }
        return total / ratingsCount;
    }
}
