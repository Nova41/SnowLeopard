package com.nova41.bukkitdev.slr.model;

/**
 * A series of doubles with a category name.
 */
public class LabeledData implements Cloneable {
    private int category;
    private double[] data;

    /**
     * Create a new labeled data
     *
     * @param category category of data
     * @param values actual data
     */
    public LabeledData(int category, double[] values) {
        this.category = category;
        this.data = values;
    }

    public int getCategory() {
        return this.category;
    }

    public double[] getData() {
        return this.data;
    }

    public void setData(double[] data) {
        this.data = data;
    }

    public void setData(int row, double data) {
        this.data[row] = data;
    }

    // vector has to be cloned by network because network would manipulate values of the vector
    public LabeledData clone() {
        try {
            return (LabeledData) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
