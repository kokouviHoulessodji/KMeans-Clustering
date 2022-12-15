package com.kmeans.kmeans;

public class ThreadDistanceCentroid extends Thread
{
    public Record record;
    public Centroid centroid;
    public static Distance distance;
    private double distValue;

    public ThreadDistanceCentroid(Record record, Centroid centroid, Distance distance)
    {
        this.record = record;
        this.centroid = centroid;
        ThreadDistanceCentroid.distance = distance;
    }

    public double getDistValue() {
        return distValue;
    }

    @Override
    public void run()
    {
        distValue = distance.calculate(record.getCoordinates(), centroid.coordinates());
        System.out.println(this + " " + (isAlive()?" is running at " : " stopped at ")+System.currentTimeMillis());
    }

}
