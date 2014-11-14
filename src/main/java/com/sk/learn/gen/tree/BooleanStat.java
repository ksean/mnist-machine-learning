package com.sk.learn.gen.tree;

import com.google.common.math.DoubleMath;
import com.sk.learn.domain.BooleanMeasurement;

public class BooleanStat
{
    private int falseCount;
    private int trueCount;


    public void accumulate(BooleanMeasurement measurement) {
        if (measurement.value()) {
            trueCount++;
        } else {
            falseCount++;
        }
    }


    public double entropy() {
        if (falseCount == 0 && trueCount == 0) {
            return Double.NaN;
        } else if (falseCount == 0 || trueCount == 0) {
            return 0;
        }

        int totalCount = falseCount + trueCount;

        double falseProb = (double) falseCount / totalCount;
        double trueProb = 1.0 - falseProb;

        double falseEntropy = falseProb * DoubleMath.log2(falseProb);
        double trueEntropy = trueProb * DoubleMath.log2(trueProb);

        return -(falseEntropy + trueEntropy);
    }
}
