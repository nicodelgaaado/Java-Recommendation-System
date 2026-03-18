import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DirectorsFilter implements Filter {
    private final Set<String> directors;

    public DirectorsFilter(String directors) {
        this.directors = new HashSet<>();
        Arrays.stream(directors.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .forEach(this.directors::add);
    }

    @Override
    public boolean satisfies(String id) {
        String movieDirectors = MovieDatabase.getDirector(id);
        if (movieDirectors.isEmpty()) {
            return false;
        }

        for (String director : movieDirectors.split(",")) {
            if (directors.contains(director.trim())) {
                return true;
            }
        }
        return false;
    }
}
