package com.sk.learn.gen;


import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;

public interface FeatureExtractor {
    FeatureVector extract(InputSample input);
}
