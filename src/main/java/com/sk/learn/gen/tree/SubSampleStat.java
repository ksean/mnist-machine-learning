package com.sk.learn.gen.tree;


import com.google.common.collect.Maps;
import com.sk.learn.domain.BooleanMeasurement;

import java.util.HashMap;
import java.util.Map;

public class SubSampleStat
{
    private int count;
    private final Map<Integer, BooleanStat> stats = new HashMap<>();


    public void accumulate(InputSubSample subSample) {
        if (stats.isEmpty()) {
            subSample.indexes().forEach(i -> {
                stats.put(i, new BooleanStat());
            });
        }

        subSample.indexes().forEach(i -> {
            BooleanMeasurement measurement = subSample.get(i);

            stats.get(i).accumulate(measurement);
        });

        count++;
    }

    public int sampleSize() {
        return count;
    }

    public Map<Integer, Double> entropies() {
        return Maps.transformValues(stats, BooleanStat::entropy);
    }
}
