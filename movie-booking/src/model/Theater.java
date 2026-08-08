package model;

public class Theater {

    private final String theaterId;
    private final String name;
    private final String city;

    public Theater(String theaterId, String name, String city) {
        this.theaterId = theaterId;
        this.name = name;
        this.city = city;
    }

    public String getTheaterId() {
        return theaterId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
