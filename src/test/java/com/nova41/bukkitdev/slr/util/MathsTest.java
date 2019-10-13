package com.nova41.bukkitdev.slr.util;

import com.nova41.bukkitdev.slr.model.LabeledData;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathsTest {

    private static final double DELTA = 1e-15;

    @Test
    public void round() {
        assertEquals(0.13, SLMaths.round(0.1256, 2, RoundingMode.HALF_UP), DELTA);
    }

    @Test
    public void normalize() {
        List<LabeledData> dataset = new ArrayList<>();
        dataset.add(new LabeledData(1, new double[]{0.1, 0.1}));
        dataset.add(new LabeledData(1, new double[]{0.3, 0.7}));
        dataset.add(new LabeledData(1, new double[]{1.6, -0.1}));
        SLMaths.normalize(dataset);
        dataset.forEach(data -> System.out.println(Arrays.toString(data.getData())));
    }

}
