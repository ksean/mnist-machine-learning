package com.sk.learn.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureVectorTest
{
    @Test
    public void emptyFeatureVectorShouldHaveSizeZero() {
        assertThat(
                FeatureVector.create().size()
        ).isEqualTo(0);
    }


    @Test
    public void singleEntryFeatureVectorShouldHaveSizeOne() {
        assertThat(
                FeatureVector.create(false).size()
        ).isEqualTo(1);
    }

    @Test
    public void singleEntryFeatureVectorShouldHaveBooleanValue() {
        assertThat(
                FeatureVector.create(false).get(0)
        ).isFalse();

        assertThat(
                FeatureVector.create(true).get(0)
        ).isTrue();
    }
}
