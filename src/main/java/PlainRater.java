import java.util.ArrayList;
import java.util.Iterator;

public class PlainRater implements Rater {
    private final String myID;
    private final ArrayList<Rating> myRatings;

    public PlainRater(String id) {
        myID = id;
        myRatings = new ArrayList<>();
    }

    @Override
    public void addRating(String item, double rating) {
        removeExistingRating(item);
        myRatings.add(new Rating(item, rating));
    }

    @Override
    public boolean hasRating(String item) {
        return getRating(item) != -1;
    }

    @Override
    public String getID() {
        return myID;
    }

    @Override
    public double getRating(String item) {
        for (Rating rating : myRatings) {
            if (rating.getItem().equals(item)) {
                return rating.getValue();
            }
        }
        return -1;
    }

    @Override
    public int numRatings() {
        return myRatings.size();
    }

    @Override
    public ArrayList<String> getItemsRated() {
        ArrayList<String> items = new ArrayList<>();
        for (Rating rating : myRatings) {
            items.add(rating.getItem());
        }
        return items;
    }

    private void removeExistingRating(String item) {
        Iterator<Rating> iterator = myRatings.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getItem().equals(item)) {
                iterator.remove();
                return;
            }
        }
    }
}
