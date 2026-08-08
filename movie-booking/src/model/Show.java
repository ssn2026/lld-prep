package model;

import java.time.LocalDateTime;

public class Show {

    private final String showId;
    private final Movie movie;
    private final Theater theater;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final double baseSeatPrice;

    public Show(String showId, Movie movie, Theater theater, Screen screen,
            LocalDateTime startTime, double baseSeatPrice) {
        this.showId = showId;
        this.movie = movie;
        this.theater = theater;
        this.screen = screen;
        this.startTime = startTime;
        this.baseSeatPrice = baseSeatPrice;
    }

    public String getShowId() {
        return showId;
    }

    public Movie getMovie() {
        return movie;
    }

    public Theater getTheater() {
        return theater;
    }

    public Screen getScreen() {
        return screen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public double getBaseSeatPrice() {
        return baseSeatPrice;
    }
}
