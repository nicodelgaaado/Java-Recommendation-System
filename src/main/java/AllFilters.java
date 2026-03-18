import java.util.ArrayList;
import java.util.List;

public class AllFilters implements Filter {
    private final ArrayList<Filter> filters;

    public AllFilters() {
        filters = new ArrayList<>();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public List<Filter> getFilters() {
        return List.copyOf(filters);
    }

    @Override
    public boolean satisfies(String id) {
        for (Filter filter : filters) {
            if (!filter.satisfies(id)) {
                return false;
            }
        }
        return true;
    }
}
