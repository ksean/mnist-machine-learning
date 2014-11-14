package com.sk.learn.domain;

public class FeatureVector
{
    public static FeatureVector createSingleFeature(int size, int activeFeature) {
        boolean[] values = new boolean[size];
        values[activeFeature] = true;
        return new FeatureVector(values);
    }

    public static FeatureVector create(boolean... values) {
        return new FeatureVector(values.clone());
    }


    private final boolean[] values;


    private FeatureVector(boolean[] values) {
        this.values = values;
    }


    public int size() {
        return values.length;
    }

    public boolean get(int index) {
        return values[index];
    }
}
