package com.nova41.bukkitdev.slr.model;

/**
 * A wrapper containing statistics about a LVQ Neural Network.
 */
public final class LVQNeuralNetworkSummary {
    // times the network has been trained
    private int epoch;

    // current step_size of the network
    private double step_size;

    // the number of input vectors and neurons in output layer
    private int input_count;
    private int output_count;

    /**
     * Create a summary for a LVQ neural network. The constructor can be accessed only by a neural network.
     *  @param epoch times the network has been trained
     * @param step_size current step_size of the network
     * @param input_count number of input vectors
     * @param output_count number of neurons in output layer
     */
    LVQNeuralNetworkSummary(int epoch, double step_size, int input_count, int output_count) {
        this.epoch = epoch;
        this.step_size = step_size;
        this.input_count = input_count;
        this.output_count = output_count;
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
        return step_size;
    }

    /**
     * Get the number of input vectors.
     *
     * @return the number of input vectors
     */
    public int getInputCount() {
        return input_count;
    }

    /**
     * Get the number of neurons in output layer.
     * @return the number of neurons in output layer
     */
    public int getOutputCount() {
        return output_count;
    }
}
