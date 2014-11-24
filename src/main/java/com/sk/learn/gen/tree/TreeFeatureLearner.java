package com.sk.learn.gen.tree;


import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;

import java.util.ArrayList;
import java.util.List;

public class TreeFeatureLearner implements FeatureLearner
{
    private List<FeatureTree> trees;


    public TreeFeatureLearner() {
        this(64);
    }

    public TreeFeatureLearner(int size) {
        trees = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            trees.add(new FeatureTree());
        }
    }


    @Override
    public void learn(InputSample sample) {
        trees.forEach(tree -> tree.learn(sample));
    }


    @Override
    public FeatureVector extract(InputSample input) {
        List<FeatureVector> vectors = new ArrayList<>();

        for (FeatureTree tree : trees) {
            int trueIndex = tree.matchingLeafIndex(input);
            FeatureVector vector = FeatureVector.createSingleFeature(tree.leafCount(), trueIndex);
            vectors.add(vector);
        }

        int featureCount = vectors.stream().mapToInt(FeatureVector::size).sum();

        boolean[] concat = new boolean[featureCount];

        int index = 0;
        for (FeatureVector vector : vectors) {
            for (int i = 0; i < vector.size(); i++) {
                concat[index] = vector.get(i);
                index++;
            }
        }

        return FeatureVector.create(concat);
    }

}
