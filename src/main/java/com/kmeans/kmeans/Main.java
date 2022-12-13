package com.kmeans.kmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Main extends Application
{
    /**
     * Cuurent state and lastState of the cluster
     */
    private final Map<Centroid, List<Record>> clusters;
    private final Map<Centroid, List<Record>> lastState;
    /**
     * List of records
     */
    private List<Record> records;
    /**
     * List of centroids
     */
    private final List<Centroid> centroids;
    /**
     * Number of centroids to generate
     */
    private final int nbCentroids = 5;
    /**
     * Number of maximum iteration before stop
     */
    private static final int nbIterationMax = 1000;
    /**
     * Number of iteration (current)
     */
    private int round;
    /**
     * Width and Height of the Chart
     */
    private static final double width = java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final double height = java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight() - 100;
    @FXML
    private ScatterChart<?, ?> ScatterChart;

    @FXML
    private BarChart<?, ?> BarChart;


    public static void main(String[] arg)
    {
        new Main();
        launch(arg);
    }

    @Override
    public void start(Stage arg0) throws Exception
    {
        draw();

        GridPane grid = new GridPane();
        grid.addColumn(0, ScatterChart);
        grid.addColumn(1, BarChart);
        Group root = new Group(grid);
        root.setAutoSizeChildren(true);
        Scene scene = new Scene(root, width, height);
        arg0.setTitle("Chart");
        arg0.setScene(scene);
        arg0.show();
    }
    public Main()
    {
        clusters = new HashMap<>();
        lastState = new HashMap<>();
        try
        {
            records = KMeans.retreiveRecordsFromDataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        centroids = KMeans.randomCentroids(records, nbCentroids);
        run();
    }

    /**
     * Run the algrithm
     */
    private void run()
    {

        while(true)
        {
            try
            {
                if(KMeans.runClustering(records, centroids, nbCentroids, new EuclideanDistance(), nbIterationMax, round, clusters, lastState))
                    break;
            } catch (InterruptedException e)
            {
                System.out.println("Exception : "+e.getMessage());
                e.printStackTrace();
            }
            round++;

        }
    }
    /**
     * Draw Chart
     */
    public void draw()
    {
        /*
         * get the minimum and maximum value for every coordinate
         */
        Map<String, Double> maxs = new HashMap<>();
        Map<String, Double> mins = new HashMap<>();

        KMeans.getMinAndMaxOfFeatures(records, maxs, mins);

        drawCentroidsAndRecordsForScatterChart(clusters, mins, maxs);
        drawCentroidsAndRecordsForBarChart(clusters);
    }

    /**
     * Draw barChart
     * @param clusters clusters
     */
    public void drawCentroidsAndRecordsForBarChart(Map<Centroid, List<Record>> clusters)
    {
        List<String> centroids = new ArrayList<>(nbCentroids);
        for(int i=0;i<nbCentroids; i++)
            centroids.add(""+(i+1));
        ObservableList<String> listCentroids = FXCollections.observableArrayList(centroids);

        //X Axis
        CategoryAxis xAxis = new CategoryAxis(listCentroids);
        xAxis.setLabel("No. of centroids");

        int maxItem = getMaxItemInOneCluster(clusters);

        //Y Axis
        NumberAxis yAxis = new NumberAxis(0, maxItem + 1, 1);
        yAxis.setLabel("Nb Observations");

        BarChart = new BarChart<>(xAxis, yAxis);
        BarChart.setTitle("Nb records per centroid");
        BarChart.setMinSize(width/2, height - 100);
        int it = 0;
        XYChart.Series series = new XYChart.Series<>();
        series.setName("Nb Observations");
        for (Map.Entry<Centroid, List<Record>> entry : clusters.entrySet())
        {
            if(entry.getValue() != null)
                series.getData().add(new XYChart.Data<>(centroids.get(it), entry.getValue().size()));
            else
                series.getData().add(new XYChart.Data(centroids.get(it), 0));


            it++;
        }
        BarChart.getData().addAll(series);
    }

    /**
     *
     * @param clusters2
     * @return
     */
    private int getMaxItemInOneCluster(Map<Centroid, List<Record>> clusters2)
    {
        List<Integer> nbItems = new ArrayList<Integer>();
        clusters2
                .forEach((key, value) -> {
                    nbItems.add(value.size());
                });
        int max = Integer.MIN_VALUE;
        for (Integer value : nbItems)
        {
            if(value > max)
                max = value;
        }
        return max;
    }

    /**
     * Draw ScatterChart
     * @param clusters cluster
     * @param mins mininums
     * @param maxs maximums
     */
    public void drawCentroidsAndRecordsForScatterChart(Map<Centroid, List<Record>> clusters, Map<String, Double> mins, Map<String, Double> maxs)
    {
        //X axis
        NumberAxis xAxis = new NumberAxis(mins.get("latitude") - 1, maxs.get("latitude") + 1, 0.5);
        xAxis.setLabel("latitude");

        //Y Axis
        NumberAxis yAxis = new NumberAxis(mins.get("longitude") - 1, maxs.get("longitude") + 1, 0.5);
        yAxis.setLabel("longitude");

        ScatterChart = new ScatterChart<>(xAxis, yAxis);
        ScatterChart.setTitle("Final Cluster");
        ScatterChart.setMinSize(width/2, height - 100);
        int it = 0;
        for (Map.Entry<Centroid, List<Record>> entry : clusters.entrySet())
        {
            XYChart.Series series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data(entry.getKey().getCoordinates("latitude"), entry.getKey().getCoordinates("longitude")));
            series.setName("Centroid " + (it + 1) + "\nx : " + entry.getKey().getCoordinates("latitude") + "\ny : " + entry.getKey().getCoordinates("longitude"));

            if(entry.getValue() != null)
            {
                for (int j = 0; j < entry.getValue().size(); j++)
                {
                    series.getData().add(new XYChart.Data(entry.getValue().get(j).getCoordinates("latitude"), (entry.getValue().get(j).getCoordinates("longitude"))));
                }
            }
            ScatterChart.getData().add(series);
            it++;
        }
    }

}

