package com.sk.learn.gen.tree;


import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;

public class TreeFeatureLearner implements FeatureLearner
{
    private FeatureTree tree;


    public TreeFeatureLearner() {
        tree = new FeatureTree();
    }


    @Override
    public void learn(InputSample sample) {
        tree.learn(sample);
    }


    @Override
    public FeatureVector extract(InputSample input) {
        int trueIndex = tree.matchingLeafIndex(input);

        return FeatureVector.createSingleFeature(tree.leaves().size(), trueIndex);
    }

}
