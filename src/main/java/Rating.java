public class Rating implements Comparable<Rating> {
    private final String item;
    private final double value;

    public Rating(String anItem, double aValue) {
        item = anItem;
        value = aValue;
    }

    public String getItem() {
        return item;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(Rating other) {
        int byValue = Double.compare(value, other.value);
        if (byValue != 0) {
            return byValue;
        }
        return item.compareTo(other.item);
    }

    @Override
    public String toString() {
        return "[" + item + ", " + value + "]";
    }
}
