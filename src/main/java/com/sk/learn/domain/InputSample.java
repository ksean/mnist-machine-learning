package com.sk.learn.domain;

public class InputSample {

    public static InputSample create(BooleanMeasurement... measurements) {
        return new InputSample(measurements.clone());
    }

    private final BooleanMeasurement[] measurements;


    private InputSample(BooleanMeasurement[] measurements) {
        this.measurements = measurements;
    }


    public int size() {
        return measurements.length;
    }

    public BooleanMeasurement measurement(int index) {
        return measurements[index];
    }

}
