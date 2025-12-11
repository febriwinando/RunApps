package tech.id.runappsandroid;
import java.util.List;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public OverviewPolyline overview_polyline;
        public List<Leg> legs;
    }

    public static class OverviewPolyline {
        public String points;
    }

    public static class Leg {
        public Distance distance;
        public Duration duration;
        public String start_address;
        public String end_address;
    }

    public static class Distance {
        public String text;
        public int value; // meters
    }

    public static class Duration {
        public String text;
        public int value; // seconds
    }
}
