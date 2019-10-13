package com.nova41.bukkitdev.slr.model;

import lombok.Getter;

/**
 * A series of doubles with a category name.
 */
public class LabeledData implements Cloneable {

    @Getter
    private int category;
    @Getter
    private double[] data;

    public LabeledData(int category, double[] values) {
        this.category = category;
        this.data = values;
    }

    public void setData(double[] data) {
        this.data = data;
    }

    public void setData(int row, double data) {
        this.data[row] = data;
    }

    // Vector has to be cloned by network because network would manipulate values of the vector
    public LabeledData clone() {
        try {
            return (LabeledData) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
