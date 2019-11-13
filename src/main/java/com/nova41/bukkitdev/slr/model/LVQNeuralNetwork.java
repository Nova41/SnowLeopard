package com.nova41.bukkitdev.slr.model;

import com.nova41.bukkitdev.slr.util.SLMaths;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A java implementation of learning-vector-quantization neural network
 *
 * References: T. Kohonen, "Improved Versions of Learning Vector
 * Quantization", International Joint Conference on Neural Networks (IJCNN),
 * 1990.
 *
 * @author Nova41
 * @version 2.0
 *
 */
public class LVQNeuralNetwork {

    // learning factors
    private double step_size;         // initial step size, recommended: 0.5
    private double step_dec_rate;    // step decrease rate, recommended: 0.99
    private double min_step_size;    // minimum step size, recommended: 0.10

    // times the network has been trained
    private int epoch = 0;

    // the number of dimensions expected from input vectors
    private int dimension;

    // Stores all labeled vector
    private List<LabeledData> vectors = new ArrayList<>();

    // The center vector of categories (lso called the output layer)
    private List<LabeledData> classCenters = new ArrayList<>();

    // Min and max value in each row in dataset
    private double[][] minMaxOfRow;

    /**
     * Create a new LVQ neural network with given learning parameters.
     *
     * @param dimension the number of dimensions expected from input vectors
     * @param step_size initial step size
     * @param step_dec_rate step decrease rate, the step size will *= step_dec_rate after each epoch
     * @param min_step_size minimum step size, the step size ceases to decrease if it is lower than this value
     */
    public LVQNeuralNetwork(int dimension, double step_size, double step_dec_rate, double min_step_size) {
        this.dimension = dimension;
        this.step_size = step_size;
        this.step_dec_rate = step_dec_rate;
        this.min_step_size = min_step_size;
    }

    // Add a new labeled vector to the network
    public void addData(LabeledData vector) {
        if (vector.getData().length != dimension)
            throw new IllegalArgumentException(String.format("Input has illegal dimensions (%d, excepted %d)", vector.getData().length, dimension));

        vectors.add(vector);
    }

    // Get distances to different class centers
    // The first one is the most similar to the input vector (returns the most similar vector's index in vectors)
    private TreeMap<Double, Integer> getDistanceToClassCenters(double[] vector) {
        // if initializeOutputLayer() is not called first
        if (classCenters.size() == 0)
            throw new IllegalStateException("Output layer is not initialized yet");

        // <distance, index in classCenters>; use TreeMap so the map is sorted naturally by keys
        TreeMap<Double, Integer> distanceToInput = new TreeMap<>();
        for (int i = 0; i <= classCenters.size() - 1; i++)
            distanceToInput.put(SLMaths.euclideanDistance(vector, classCenters.get(i).getData()), i);
        return distanceToInput;
    }

    // Initialize classCenters according to how many classes there are.
    // The network converges faster by randomly picking a vector from (instead of generating a random vector for) each class as the class's center
    public void initializeOutputLayer() {
        // reset epoch, because we drop the knowledge the output layer possesses and start from scratch
        epoch = 0;

        // get all categories appeared
        vectors.stream()
                .map(LabeledData::getCategory)      // map vector list to category list
                .collect(Collectors.toSet())        // get all categories
                .forEach(category -> vectors.stream()
                        .filter(vector -> vector.getCategory() == category)
                        .findAny()  // randomly pick a vector and set it the center of its class
                        .ifPresent(randomVector -> classCenters.add(randomVector.clone()))  // create class center
                );
    }

    // Normalize the dataset!
    public void normalize() {
        minMaxOfRow = SLMaths.normalize(vectors);
    }

    // start train
    public void train() {
        // for every input vector
        for (LabeledData vector : vectors) {
            // calculate its distance to nearest class center and multiply it with step_size
            LabeledData nearestOutput = classCenters.get(getDistanceToClassCenters(vector.getData()).firstEntry().getValue());
            double[] distToNearestOutput = SLMaths.multiply(SLMaths.subtract(vector.getData(), nearestOutput.getData()), step_size);

            // pull the nearest class center closer by the distance above if the center and the vector have the same category, otherwise farther
            if (vector.getCategory() == nearestOutput.getCategory())
                nearestOutput.setData(SLMaths.add(nearestOutput.getData(), distToNearestOutput));
            else
                nearestOutput.setData(SLMaths.subtract(nearestOutput.getData(), distToNearestOutput));
        }

        // decrease step_size until it is smaller than or equal to min_step_size
        if (step_size > min_step_size)
            step_size *= step_dec_rate;
        else
            step_size = min_step_size;

        epoch++;
    }

    // Classify a data and return the classification result
    public LVQNeuralNetworkPredictResult predict(double[] vector) {
        if (classCenters.size() == 0)
            throw new IllegalStateException("Output layer is not initialized yet");

        // normalize the input data
        double[] vectorNormalized = vector.clone();
        for (int i = 0; i <= vector.length - 1; i++)
            vectorNormalized[i] = SLMaths.normalize(vector[i], minMaxOfRow[i][0], minMaxOfRow[i][1]);

        return new LVQNeuralNetworkPredictResult(getDistanceToClassCenters(vectorNormalized));
    }

    // print the distribution of vectors; just for fun
    // the position of points are round thus not accurate, and the code is a huge mess
    void printVectors() {
        if (dimension != 2)
            throw new IllegalArgumentException("The network does not support printing vectors with more than 2 dimensions");

        System.out.println("Input vectors: ");
        int[][] outputImage = new int[vectors.size()][vectors.size()];
        for (LabeledData vector : vectors)
            outputImage[(int) (vector.getData()[0] * 10)][(int) (vector.getData()[1] * 10)] = vector.getCategory() + 1;

        // draw
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");
        for (int i = 0; i <= outputImage.length - 1; i++) {
            System.out.print("|");
            for (int j = 0; j <= outputImage.length - 1; j++)
                System.out.print(outputImage[i][j] == 0 ? "  " : outputImage[i][j] + " ");
            System.out.print("|\n");
        }
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");

        // clear outputImage[][] for drawing output layer
        for (int i = 0; i <= outputImage.length - 1; i++)
            for (int j = 0; j <= outputImage.length - 1; j++)
                outputImage[i][j] = 0;
        // draw
        System.out.println("Output layer: ");
        for (LabeledData vector : classCenters)
            outputImage[(int) Math.round(vector.getData()[0] * 10)][(int) Math.round(vector.getData()[1] * 10)] = vector.getCategory() + 1;
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");
        for (int i = 0; i <= outputImage.length - 1; i++) {
            System.out.print("|");
            for (int j = 0; j <= outputImage.length - 1; j++)
                System.out.print(outputImage[i][j] == 0 ? "  " : outputImage[i][j] + " ");
            System.out.print("|\n");
        }
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");

        /* The output would be something like:

            Output layer:
            +--------------------+
            |                    |
            |                    |
            |                    |
            |                    |
            |          1         |
            |                    |
            |                    |
            |                    |
            |            2       |
            |                    |
            +--------------------+
        */
    }

    // print network info
    public void printStats(Logger logger) {
        logger.info("Current Epoch: " + epoch + ", Current step size: " + step_size);
        logger.info("Output layer:");
        classCenters.forEach(vector -> logger.info(" - " + vector.getCategory() + " " +Arrays.toString(vector.getData())));
        logger.info("Dataset (normalized):");
        vectors.forEach(vector -> logger.info(" - " + vector.getCategory() + " " +Arrays.toString(vector.getData())));
    }

    // Get summary statistics of the network
    public LVQNeuralNetworkSummary getSummaryStatistics() {
        return new LVQNeuralNetworkSummary(epoch, step_size, vectors.size(), classCenters.size());
    }
}
