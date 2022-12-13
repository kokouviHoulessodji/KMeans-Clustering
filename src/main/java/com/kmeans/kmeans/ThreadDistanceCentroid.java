package com.kmeans.kmeans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadDistanceCentroid extends Thread
{
    public static Map<Centroid, Double> distCentroid;
    public static Record record;
    public static List<Centroid> centroids;
    public static Distance distance;

    public ThreadDistanceCentroid(Record record, List<Centroid> centroids, Distance distance)
    {
        ThreadDistanceCentroid.record = record;
        ThreadDistanceCentroid.centroids = centroids;
        ThreadDistanceCentroid.distCentroid = new HashMap<Centroid, Double>(centroids.size());
        ThreadDistanceCentroid.distance = distance;
    }

    @Override
    public void run()
    {
        centroids
                .parallelStream()
                .forEach(centroid -> {
                    ThreadDistanceCentroid.distCentroid
                            .put(centroid, distance.calculate(record.getCoordinates(), centroid.coordinates()));
                    System.out.println(this + " is runing");
                });
    }

}
