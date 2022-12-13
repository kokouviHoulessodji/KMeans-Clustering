package com.kmeans.kmeans;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Encapsulates an implementation of KMeans clustering algorithm.
 *
 * @author
 */
public class KMeans
{
    /**
     * Dataset path on your disk
     */
    private static final String dataset = "src\\main\\resources\\com\\kmeans\\kmeans\\dataset.csv";
    /**
     * Will be used to generate random numbers.
     */
    private static final Random random = new Random();

    /**
     * read data(record) in the dataset file
     * @return  list of records
     * @throws IOException exception
     */
    public static List<Record> retreiveRecordsFromDataset() throws IOException
    {
        List<Record> records = new ArrayList<>();
        try
        {
            File file = new File(dataset);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            String[] tempArr;
            ArrayList<String> headers = getHeaders(line = br.readLine());

            while((line = br.readLine()) != null)
            {
                String description = "";
                tempArr = line.split(";");
                Map<String, Double> features = new HashMap<String, Double>((tempArr.length - 1));
                int iterC = 0;
                for(String value : tempArr)
                {
                    if(iterC == 0)
                        description = value;
                    else
                        features.put(headers.get(iterC), Double.valueOf(value));
                    iterC++;
                }
                records.add(new Record(description, features));
            }
            br.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return records;
    }

    /**
     * print clusters configuration
     * @param clusters the clusters
     * @param iter current iteration
     */
    public static void printClustersConfiguration(Map<Centroid, List<Record>> clusters, int iter)
    {
        System.out.println("------------------------------ Iteration " + iter + "-----------------------------------");
        clusters.forEach((key, value) -> {
            System.out.println("------------------------------ CLUSTER -----------------------------------");

            System.out.println(sortedCentroid(key));
            String members = String.join(", ", value
                    .parallelStream()
                    .map(Record::getDescription)
                    .collect(toSet()));
            System.out.println("nb obs : " +value.size());
            System.out.print(members);

            System.out.println();
            System.out.println();
        });
    }

    /**
     *
     * @param key centroid
     * @return new sorted centroid
     */
    private static Centroid sortedCentroid(Centroid key)
    {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(key
                .coordinates()
                .entrySet());
        entries.sort((e1, e2) -> e2
                .getValue()
                .compareTo(e1.getValue()));

        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries)
            sorted.put(entry.getKey(), entry.getValue());

        return new Centroid(sorted);
    }

    /**
     * Get Headers from excel row
     * @param line data
     * @return list of headers
     */
    public static ArrayList<String> getHeaders(String line)
    {
        String [] tempArr = line.split(";");
        ArrayList<String> htParam = new ArrayList<String>(tempArr.length);
        //htParam.add("Head");
        // Loop over the columns
        Collections.addAll(htParam, tempArr);
        return htParam;
    }

    /**
     * Performs the K-Means clustering algorithm on the given dataset.
     *
     * @param records       The dataset.
     * @param k             Number of Clusters.
     * @param distance      To calculate the distance between two items.
     * @param maxIterations Upper bound for the number of iterations.
     * @param round         current iteration
     * @param lastState     cluster at last iteration
     * @param clusters      current cluster
     * @return              true or false(should continue the algorithm or not)
     * @throws              InterruptedException exception
     */
    public static boolean runClustering(List<Record> records, List<Centroid> centroids, int k,
                                        Distance distance, int maxIterations, int round, Map<Centroid, List<Record>> clusters,
                                        Map<Centroid, List<Record>> lastState) throws InterruptedException
    {
        applyPreconditions(records, k, distance, maxIterations);

        boolean isLastIteration = round == maxIterations - 1;

        // 1ST STEP : in each iteration we should find the nearest centroid for each record

        for (Record record : records)
        {
            Centroid centroid = nearestCentroid(record, centroids, distance);
            assignToCluster(clusters, record, centroid);
        }

        // if the assignment does not change, then the algorithm terminates
        boolean shouldTerminate = isLastIteration || clusters.equals(lastState);
        lastState.clear();

        lastState.putAll(clusters);

        if (shouldTerminate)
        {
            printClustersConfiguration(clusters, round);
            return true;
        }

        // 2ND STEP : at the end of each iteration we should relocate the centroids
        centroids = relocateCentroids(clusters);
        printClustersConfiguration(clusters, round);
        clusters.clear();
        return false;
    }

    /**
     * Move all cluster centroids to the average of all assigned features.
     *
     * @param clusters The current cluster configuration.
     * @return Collection of new and relocated centroids.
     */
    private static List<Centroid> relocateCentroids(Map<Centroid, List<Record>> clusters)
    {
        return clusters
                .entrySet()
                .parallelStream()
                .map(e -> average(e.getKey(), e.getValue()))
                .collect(toList());
    }

    /**
     * Moves the given centroid to the average position of all assigned features. If
     * the centroid has no feature in its cluster, then there would be no need for a
     * relocation. Otherwise, for each entry we calculate the average of all records
     * first by summing all the entries and then dividing the final summation value by
     * the number of records.
     *
     * @param centroid The centroid to move.
     * @param records  The assigned features.
     * @return The moved centroid.
     */
    private static Centroid average(Centroid centroid, List<Record> records) {
        // if this cluster is empty, then we shouldn't move the centroid
        if (records == null || records.isEmpty())
            return centroid;

        // Since some records don't have all possible attributes, we initialize
        // average coordinates equal to current centroid coordinates
        Map<String, Double> average = centroid.coordinates();

        // The average function works correctly if we clear all coordinates corresponding
        // to present record attributes
        records
                .parallelStream()
                .flatMap(e -> e
                        .getCoordinates()
                        .keySet()
                        .parallelStream())
                .forEach(k -> average.put(k, 0.0));

        for (Record record : records)
        {
            record
                    .getCoordinates()
                    .forEach((k, v) -> average.compute(k, (k1, currentValue) -> v + currentValue));
        }

        average.forEach((k, v) -> average.put(k, v / records.size()));

        return new Centroid(average);
    }

    /**
     * Assigns a feature vector to the given centroid. If this is the first assignment for this centroid,
     * first we should create the list.
     *
     * @param clusters The current cluster configuration.
     * @param record   The feature vector.
     * @param centroid The centroid.
     */
    private static void assignToCluster(Map<Centroid, List<Record>> clusters, Record record, Centroid centroid)
    {
        clusters.compute(centroid, (key, list) -> {
            if (list == null)
                list = new ArrayList<>();

            list.add(record);
            return list;
        });
    }

    /**
     * With the help of the given distance calculator, iterates through centroids and finds the
     * nearest one to the given record.
     *
     * @param record    The feature vector to find a centroid for.
     * @param centroids Collection of all centroids.
     * @param distance  To calculate the distance between two items.
     * @return The nearest centroid to the given feature vector.
     */
    private static Centroid nearestCentroid(Record record, List<Centroid> centroids, Distance distance)
    {
        //Parcourt parallel
        double minimumDistance = Double.MAX_VALUE;
        Centroid nearest = null;

        Thread t = new ThreadDistanceCentroid(record, centroids, distance);
        try
        {
            long lastTime = System.currentTimeMillis();
            t.start();
            t.join();
            System.err.println(record + " Time spend for distance calculation : " + (System.currentTimeMillis() - lastTime));
        }
        catch (Exception e)
        {
            e.getStackTrace();
        }

        Map<Centroid, Double> distCentroids = ThreadDistanceCentroid.distCentroid;
        for (Map.Entry<Centroid, Double> entry : distCentroids.entrySet())
        {
            if (entry.getValue() < minimumDistance)
            {
                minimumDistance = entry.getValue();
                nearest = entry.getKey();
            }
        }
        System.out.println("Nearest centroid : "+nearest);
        return nearest;
    	/*Parcourt séquentiel

        double minimumDistance = Double.MAX_VALUE;
        Centroid nearest = null;
        long lastTime = System.currentTimeMillis();
        for (Centroid centroid : centroids)
        {
            double currentDistance = distance.calculate(record.getCoordinates(), centroid.getCoordinates());

            if (currentDistance < minimumDistance)
            {
                minimumDistance = currentDistance;
                nearest = centroid;
            }
        }
        System.err.println(record + " Time spend for distance calculation : " + (System.currentTimeMillis() - lastTime));
		System.out.println("Nearest centroid : "+nearest);
        return nearest;*/
    }

    /**
     * Generates k random centroids. Before kicking-off the centroid generation process,
     * first we calculate the possible value range for each attribute. Then when
     * we're going to generate the centroids, we generate random coordinates in
     * the [min, max] range for each attribute.
     *
     * @param records The dataset which helps to calculate the [min, max] range for
     *                each attribute.
     * @param k       Number of clusters.
     * @return Collections of randomly generated centroids.
     */
    public static List<Centroid> randomCentroids(List<Record> records, int k)
    {
        List<Centroid> centroids = new ArrayList<>(k);
        Map<String, Double> maxs = new HashMap<>();
        Map<String, Double> mins = new HashMap<>();
        getMinAndMaxOfFeatures(records, maxs, mins);
        Set<String> attributes = records
                .parallelStream()
                .flatMap(e -> e
                        .getCoordinates()
                        .keySet()
                        .parallelStream())
                .collect(toSet());
        for (int i = 0; i < k; i++)
        {
            Map<String, Double> coordinates = new HashMap<>();
            for (String attribute : attributes)
            {
                double max = maxs.get(attribute);
                double min = mins.get(attribute);
                coordinates.put(attribute, random.nextDouble() * (max - min) + min);
            }
            if(!centroids.contains(new Centroid(coordinates)))
                centroids.add(new Centroid(coordinates));
            else
                i--;
        }

        return centroids;
    }

    public static void getMinAndMaxOfFeatures(List<Record> records, Map<String, Double> maxs, Map<String, Double> mins)
    {
        for (Record record : records)  {
            record
                    .getCoordinates()
                    .forEach((key, value) -> {
                        // compares the value with the current max and choose the bigger value between them
                        maxs.compute(key, (k1, max) -> max == null || value > max ? value : max);

                        // compare the value with the current min and choose the smaller value between them
                        mins.compute(key, (k1, min) -> min == null || value < min ? value : min);
                    });
        }
    }

    private static void applyPreconditions(List<Record> records, int k, Distance distance, int maxIterations)
    {
        if (records == null || records.isEmpty())
            throw new IllegalArgumentException("The dataset can't be empty");

        if (k <= 1)
            throw new IllegalArgumentException("It doesn't make sense to have less than or equal to 1 cluster");

        if (distance == null)
            throw new IllegalArgumentException("The distance calculator is required");

        if (maxIterations <= 0)
            throw new IllegalArgumentException("Max iterations should be a positive number");
    }
}

