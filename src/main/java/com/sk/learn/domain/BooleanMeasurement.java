package com.sk.learn.domain;


public class BooleanMeasurement
{
    private static final BooleanMeasurement FALSE = new BooleanMeasurement(false);
    private static final BooleanMeasurement TRUE = new BooleanMeasurement(true);

    public static BooleanMeasurement create(boolean value) {
        return value ? TRUE : FALSE;
    }


    private final boolean value;


    private BooleanMeasurement(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }
}
