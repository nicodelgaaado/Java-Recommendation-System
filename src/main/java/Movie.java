public class Movie {
    private final String id;
    private final String title;
    private final int year;
    private final String genres;
    private final String director;
    private final String country;
    private final int minutes;
    private final String poster;

    public Movie(String anID, String aTitle, String aYear, String theGenres) {
        this(anID, aTitle, Integer.parseInt(aYear.trim()), theGenres, "", "", 0, "N/A");
    }

    public Movie(String anID, String aTitle, String aYear, String theGenres, String aDirector,
                 String aCountry, String aPoster, int theMinutes) {
        this(anID, aTitle, Integer.parseInt(aYear.trim()), theGenres, aDirector, aCountry, theMinutes, aPoster);
    }

    public Movie(String anID, String aTitle, int aYear, String theGenres, String aDirector,
                 String aCountry, int theMinutes, String aPoster) {
        id = anID.trim();
        title = aTitle.trim();
        year = aYear;
        genres = theGenres.trim();
        director = aDirector.trim();
        country = aCountry.trim();
        minutes = theMinutes;
        poster = aPoster.trim();
    }

    public String getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public String getGenres() {
        return genres;
    }

    public String getCountry() {
        return country;
    }

    public String getDirector() {
        return director;
    }

    public String getPoster() {
        return poster;
    }

    public int getMinutes() {
        return minutes;
    }

    @Override
    public String toString() {
        return "Movie[id=%s, title=%s, year=%d, genres=%s]".formatted(id, title, year, genres);
    }
}
