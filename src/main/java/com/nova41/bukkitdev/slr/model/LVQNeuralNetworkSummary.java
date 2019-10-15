package com.nova41.bukkitdev.slr.model;

/**
 * A wrapper containing statistics about a LVQ Neural Network.
 */
public final class LVQNeuralNetworkSummary {

    // Times the network has been trained
    private int epoch;

    // Current step_size of the network
    private double stepSize;

    // The number of input vectors and neurons in output layer
    private int inputCount;
    private int outputCount;

    /**
     * Create a summary for a LVQ neural network. The constructor can be accessed only by a neural network.
     *
     * @param epoch       times the network has been trained
     * @param stepSize    current stepSize of the network
     * @param inputCount  number of input vectors
     * @param outputCount number of neurons in output layer
     */
    LVQNeuralNetworkSummary(int epoch, double stepSize, int inputCount, int outputCount) {
        this.epoch = epoch;
        this.stepSize = stepSize;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
    }

    /**
     * Get times the network has been trained.
     *
     * @return times the network has been trained.
     */
    public int getEpoch() {
        return epoch;
    }

    /**
     * Get current step size of the network.
     *
     * @return current step size of the network
     */
    public double getCurrentStepSize() {
        return stepSize;
    }

    /**
     * Get the number of input vectors.
     *
     * @return the number of input vectors
     */
    public int getInputCount() {
        return inputCount;
    }

    /**
     * Get the number of neurons in output layer.
     *
     * @return the number of neurons in output layer
     */
    public int getOutputCount() {
        return outputCount;
    }

}
