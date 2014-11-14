package com.sk.learn.gen.tree;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sk.learn.domain.BooleanMeasurement;
import com.sk.learn.domain.InputSample;

import java.util.Map;
import java.util.Set;

public class InputSubSample
{
    public static InputSubSample create(InputSample input) {
        ImmutableMap.Builder<Integer, BooleanMeasurement> all = ImmutableMap.builder();

        for (int i = 0; i < input.size(); i++) {
            all.put(i, input.measurement(i));
        }

        return new InputSubSample(all.build());
    }

    private final ImmutableMap<Integer, BooleanMeasurement> measurements;


    private InputSubSample(Map<Integer, BooleanMeasurement> measurements) {
        this.measurements = ImmutableMap.copyOf(measurements);
    }


    public boolean isEmpty() {
        return measurements.isEmpty();
    }

    public Set<Integer> indexes() {
        return measurements.keySet();
    }

    public BooleanMeasurement get(int index) {
        return measurements.get(index);
    }


    public InputSubSample remove(int index) {
        return new InputSubSample(
                Maps.filterKeys(measurements, i -> i != index));
    }
}
