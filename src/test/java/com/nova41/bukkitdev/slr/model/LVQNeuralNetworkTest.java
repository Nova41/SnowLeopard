package com.nova41.bukkitdev.slr.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LVQNeuralNetworkTest {

    @Test
    public void testTrain() {
        LVQNeuralNetwork neuralNetwork = new LVQNeuralNetwork(
                2, 0.5, 0.99, 0.10);

        neuralNetwork.addData(new LabeledData(0, new double[]{0.3, 0.5}));
        neuralNetwork.addData(new LabeledData(0, new double[]{-0.1, -0.2}));
        neuralNetwork.addData(new LabeledData(0, new double[]{0.4, 0.6}));
        neuralNetwork.addData(new LabeledData(0, new double[]{0.6, 0.2}));
        neuralNetwork.addData(new LabeledData(0, new double[]{0.3, 0.1}));
        neuralNetwork.addData(new LabeledData(1, new double[]{0.8, 0.6}));
        neuralNetwork.addData(new LabeledData(1, new double[]{0.7, 0.9}));
        neuralNetwork.addData(new LabeledData(1, new double[]{0.5, 0.8}));
        neuralNetwork.addData(new LabeledData(1, new double[]{0.4, 0.8}));
        neuralNetwork.addData(new LabeledData(1, new double[]{0.6, 0.3}));

        neuralNetwork.initializeOutputLayer();
        neuralNetwork.normalize();

        for (int i = 1; i <= 100; i++)
            neuralNetwork.train();

        double[] testData = new double[]{0.4, 0.4};

        LVQNeuralNetworkPredictResult predictResult = neuralNetwork.predict(testData);
        assertEquals(predictResult.getCategory(), 0);
        System.out.printf("%s -> LVQNNPredictResult[best_match=%s,distance=%s]",
                Arrays.toString(testData), predictResult.getCategory(), predictResult.getDifference());
    }

}
