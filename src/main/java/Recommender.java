import java.util.ArrayList;

public interface Recommender {
    ArrayList<String> getItemsToRate();

    void printRecommendationsFor(String webRaterID);
}
