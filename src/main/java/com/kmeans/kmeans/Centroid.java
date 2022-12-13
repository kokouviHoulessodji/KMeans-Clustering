package com.kmeans.kmeans;


import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates all coordinates for a particular cluster centroid.
 *
 * @param coordinates The centroid coordinates.
 */
public record Centroid(Map<String, Double> coordinates) {

    public double getCoordinates(String key) {
        if (key != null && key.trim().length() > 0)
            return coordinates.get(key);
        else
            return 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Centroid centroid = (Centroid) o;
        return Objects.equals(coordinates(), centroid.coordinates());
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates());
    }

    @Override
    public String toString() {
        return "Centroid " + coordinates;
    }
}
