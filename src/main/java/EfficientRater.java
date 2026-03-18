import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EfficientRater implements Rater {
    private final String myID;
    private final HashMap<String, Rating> myRatings;

    public EfficientRater(String id) {
        myID = id;
        myRatings = new HashMap<>();
    }

    @Override
    public void addRating(String item, double rating) {
        myRatings.put(item, new Rating(item, rating));
    }

    @Override
    public boolean hasRating(String item) {
        return myRatings.containsKey(item);
    }

    @Override
    public String getID() {
        return myID;
    }

    @Override
    public double getRating(String item) {
        Rating rating = myRatings.get(item);
        return rating == null ? -1 : rating.getValue();
    }

    @Override
    public int numRatings() {
        return myRatings.size();
    }

    @Override
    public ArrayList<String> getItemsRated() {
        return new ArrayList<>(myRatings.keySet());
    }

    public ArrayList<Rating> getRatings() {
        return new ArrayList<>(myRatings.values());
    }

    public Map<String, Rating> asMap() {
        return Map.copyOf(myRatings);
    }
}
