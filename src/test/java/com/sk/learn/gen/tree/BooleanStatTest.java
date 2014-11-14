package com.sk.learn.gen.tree;

import com.sk.learn.domain.BooleanMeasurement;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BooleanStatTest
{
    @Test
    public void entropyWithoutDataIsNotDefined() {
        BooleanStat stat = new BooleanStat();

        assertThat(stat.entropy()).isNaN();
    }


    @Test
    public void entropyOfExclusivelyFalseMeasurementsIsZero() {
        BooleanStat stat = new BooleanStat();

        stat.accumulate(BooleanMeasurement.create(false));

        assertThat(stat.entropy()).isZero();

        stat.accumulate(BooleanMeasurement.create(false));

        assertThat(stat.entropy()).isZero();
    }


    @Test
    public void entropyOfExclusivelyTrueMeasurementsIsZero() {
        BooleanStat stat = new BooleanStat();

        stat.accumulate(BooleanMeasurement.create(true));

        assertThat(stat.entropy()).isZero();
    }


    @Test
    public void entropyOfUniformlyDistributedMeasurementsIsOne() {
        BooleanStat stat = new BooleanStat();

        stat.accumulate(BooleanMeasurement.create(true));
        stat.accumulate(BooleanMeasurement.create(false));

        assertThat(stat.entropy()).isEqualTo(1);

        stat.accumulate(BooleanMeasurement.create(true));
        stat.accumulate(BooleanMeasurement.create(false));

        assertThat(stat.entropy()).isEqualTo(1);
    }


    @Test
    public void entropyOfDecreasesAsMeasurementsBecomeMoreUniform() {
        BooleanStat stat = new BooleanStat();

        stat.accumulate(BooleanMeasurement.create(true));
        stat.accumulate(BooleanMeasurement.create(false));
        stat.accumulate(BooleanMeasurement.create(false));

        double oneThirdEntropy = stat.entropy();
        assertThat(oneThirdEntropy).isLessThan(1);

        stat.accumulate(BooleanMeasurement.create(false));

        assertThat(stat.entropy()).isLessThan(oneThirdEntropy);
    }
}
