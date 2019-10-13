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
 * A Java implementation of learning-vector-quantization neural network.
 *
 * References: T. Kohonen, "Improved Versions of Learning Vector
 * Quantization", International Joint Conference on Neural Networks (IJCNN),
 * 1990.
 *
 * @author  Nova41
 * @version 2.0
 */
public class LVQNeuralNetwork {

    // Stores all labeled vector
    private final List<LabeledData> vectors = new ArrayList<>();

    // The center vector of categories (lso called the output layer)
    private final List<LabeledData> classCenters = new ArrayList<>();

    private double stepSize;        // initial step size, recommended: 0.5
    private double stepDecRate;     // step decrease rate, recommended: 0.99
    private double minStepSize;     // minimum step size, recommended: 0.10

    // Times the network has been trained
    private int epoch = 0;

    // The number of dimensions expected from input vectors
    private int dimension;

    // Min and max value in each row in dataset
    private double[][] minMaxOfRow;

    /**
     * Create a new LVQ neural network with given learning parameters.
     *
     * @param dimension     the number of dimensions expected from input vectors
     * @param stepSize      initial step size
     * @param stepDecRate   step decrease rate, the step size will *= stepDecRate after each epoch
     * @param minStepSize   minimum step size, the step size ceases to decrease if it is lower than this value
     */
    public LVQNeuralNetwork(int dimension, double stepSize, double stepDecRate, double minStepSize) {
        this.dimension = dimension;
        this.stepSize = stepSize;
        this.stepDecRate = stepDecRate;
        this.minStepSize = minStepSize;
    }

    // Add a new labeled vector to the network
    public LVQNeuralNetwork addData(LabeledData vector) {
        if (vector.getData().length != dimension)
            throw new IllegalArgumentException(String.format(
                    "Input has illegal dimensions (%d, excepted %d)", vector.getData().length, dimension));

        vectors.add(vector);
        return this;
    }

    // Get distances to different class centers
    // First one is most similar to the input vector (returns the most similar vector's index in vectors)
    private TreeMap<Double, Integer> getDistanceToClassCenters(double[] vector) {
        // If initializeOutputLayer() is not called first
        if (classCenters.size() == 0)
            throw new IllegalStateException("Output layer is not initialized yet");

            TreeMap<Double, Integer> distanceToInput = new TreeMap<>();
        for (int i = 0; i <= classCenters.size() - 1; i++)
            distanceToInput.put(SLMaths.euclideanDistance(vector, classCenters.get(i).getData()), i);
        return distanceToInput;
    }

    // Initialize classCenters according to how many classes there are
    // The network converges faster by randomly picking a vector from each class as the class's center
    public LVQNeuralNetwork initializeOutputLayer() {
        // Reset epoch, because we drop the knowledge the output layer possesses and start from scratch
        epoch = 0;

        // Get all categories appeared
        vectors.stream()
                .map(LabeledData::getCategory)      // Map vector list to category list
                .collect(Collectors.toSet())        // Get all categories
                .forEach(category -> vectors.stream()
                        .filter(vector -> vector.getCategory() == category)
                        .findAny()  // Randomly pick a vector and set it the center of its class
                        .ifPresent(randomVector -> {
                            try {
                                classCenters.add(randomVector.clone());
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }
                        }));
        return this;
    }

    // Normalize the dataset
    public LVQNeuralNetwork normalize() {
        minMaxOfRow = SLMaths.normalize(vectors);
        return this;
    }

    // Start training
    public LVQNeuralNetwork train() {
        // For every input vector
        for (LabeledData vector : vectors) {
            // Calculate its distance to nearest class center and multiply it with stepSize
            LabeledData nearestOutput = classCenters.get(
                    getDistanceToClassCenters(vector.getData()).firstEntry().getValue());

            double[] distToNearestOutput = SLMaths.multiply(SLMaths.subtract(
                    vector.getData(), nearestOutput.getData()), stepSize);

            // Pull the nearest class center closer by the distance above
            // if the center and the vector have the same category, otherwise farther
            if (vector.getCategory() == nearestOutput.getCategory())
                nearestOutput.setData(SLMaths.add(nearestOutput.getData(), distToNearestOutput));
            else
                nearestOutput.setData(SLMaths.subtract(nearestOutput.getData(), distToNearestOutput));
        }

        // Decrease step_size until it is smaller than or equal to min_step_size
        if (stepSize > minStepSize)
            stepSize *= stepDecRate;
        else
            stepSize = minStepSize;

        epoch++;
        return this;
    }

    // Classify a data and return the classification result
    public LVQNeuralNetworkPredictResult predict(double[] vector) {
        if (classCenters.size() == 0)
            throw new IllegalStateException("Output layer is not initialized yet");

        // Normalize the input data
        double[] vectorNormalized = vector.clone();
        for (int i = 0; i <= vector.length - 1; i++)
            vectorNormalized[i] = SLMaths.normalize(vector[i], minMaxOfRow[i][0], minMaxOfRow[i][1]);

        return new LVQNeuralNetworkPredictResult(getDistanceToClassCenters(vectorNormalized));
    }

    // The position of points are round thus not accurate, and the code is a huge mess
    void printVectors() {
        if (dimension != 2)
            throw new IllegalArgumentException("The network does not support printing"
                    + " vectors with more than 2 dimensions");

        System.out.println("Input vectors: ");
        int[][] outputImage = new int[vectors.size()][vectors.size()];

        // draw
        for (LabeledData vector : vectors)
            outputImage[(int) (vector.getData()[0] * 10)][(int) (vector.getData()[1] * 10)]
                    = vector.getCategory() + 1;
        drawLayer(outputImage);

        // clear outputImage[][] for drawing output layer
        for (int i = 0; i <= outputImage.length - 1; i++)
            for (int j = 0; j <= outputImage.length - 1; j++)
                outputImage[i][j] = 0;

        // draw
        System.out.println("Output layer: ");
        for (LabeledData vector : classCenters)
            outputImage[(int) Math.round(vector.getData()[0] * 10)][(int) Math.round(vector.getData()[1] * 10)]
                    = vector.getCategory() + 1;
        drawLayer(outputImage);

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

    public void drawLayer(int[][] outputImage) {
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");
        for (int i = 0; i <= outputImage.length - 1; i++) {
            System.out.print("|");
            for (int j = 0; j <= outputImage.length - 1; j++)
                System.out.print(outputImage[i][j] == 0 ? "  " : outputImage[i][j] + " ");
            System.out.print("|\n");
        }
        System.out.println("+" + StringUtils.repeat("--", vectors.size()) + "+");
    }

    // print network info
    public final void printStats(Logger logger) {
        logger.info("Current Epoch: " + epoch + ", Current step size: " + stepSize);
        logger.info("Output layer:");
        classCenters.forEach(vector -> logger.info(" - " + vector.getCategory() + " "
                + Arrays.toString(vector.getData())));
        logger.info("Dataset (normalized):");
        vectors.forEach(vector -> logger.info(" - " + vector.getCategory() + " "
                + Arrays.toString(vector.getData())));
    }

    // get summary statistics of the network
    public final LVQNeuralNetworkSummary getSummaryStatistics() {
        return new LVQNeuralNetworkSummary(epoch, stepSize, vectors.size(), classCenters.size());
    }

}
