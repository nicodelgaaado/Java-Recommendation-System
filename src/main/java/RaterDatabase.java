import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaterDatabase {
    private static Map<String, Rater> ourRaters;
    private static String ourSource;

    private static void initialize() {
        if (ourRaters == null) {
            ourRaters = new LinkedHashMap<>();
        }
    }

    public static void initialize(String filename) {
        if (ourRaters != null && normalize(filename).equals(ourSource)) {
            return;
        }

        initialize();
        ourRaters.clear();
        addRatings(filename);
        ourSource = normalize(filename);
    }

    public static void reset() {
        ourRaters = null;
        ourSource = null;
    }

    public static void addRatings(String filename) {
        initialize();
        List<Map<String, String>> rows = CsvUtils.readRows(filename);
        for (Map<String, String> row : rows) {
            addRaterRating(
                    row.get("rater_id"),
                    row.get("movie_id"),
                    Double.parseDouble(row.get("rating"))
            );
        }
    }

    public static void addRaterRating(String raterID, String movieID, double rating) {
        initialize();
        Rater rater = ourRaters.computeIfAbsent(raterID, EfficientRater::new);
        rater.addRating(movieID, rating);
    }

    public static Rater getRater(String id) {
        initialize();
        return ourRaters.get(id);
    }

    public static ArrayList<Rater> getRaters() {
        initialize();
        return new ArrayList<>(ourRaters.values());
    }

    public static boolean containsRater(String id) {
        initialize();
        return ourRaters.containsKey(id);
    }

    public static int size() {
        initialize();
        return ourRaters.size();
    }

    private static String normalize(String filename) {
        return filename.replace('\\', '/');
    }
}
