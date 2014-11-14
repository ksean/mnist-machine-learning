package com.sk.learn.gen.tree;

import com.google.common.collect.Ordering;
import com.sk.learn.domain.BooleanMeasurement;
import com.sk.learn.domain.InputSample;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class FeatureNode {
    private final FeatureTree tree;
    private final int id;

    private SubSampleStat sampleStat = new SubSampleStat();

    private Optional<Integer> splitIndex = Optional.empty();
    private Optional<FeatureNode> leftChild = Optional.empty();
    private Optional<FeatureNode> rightChild = Optional.empty();


    public FeatureNode(FeatureTree tree, int id) {
        this.tree = tree;
        this.id = id;
    }

    public int identifyLeaf(InputSubSample input) {
        if (isLeaf()) {
            return id;
        }

        return matchingChild(input).identifyLeaf(input);
    }


    public void learn(InputSubSample input) {
        if (isLeaf()) {
            accumulateStats(input);
        } else {
            learnInChildNodes(input);
        }
    }

    private boolean isLeaf() {
        return ! splitIndex.isPresent();
    }


    private void learnInChildNodes(InputSubSample input) {
        FeatureNode matchingChild = matchingChild(input);

        InputSubSample remainingInput = input.remove(splitIndex.get());

        matchingChild.learn(remainingInput);
    }

    private FeatureNode matchingChild(InputSubSample input) {
        BooleanMeasurement measurement = input.get(splitIndex.get());
        return (measurement.value() ? leftChild : rightChild).get();
    }


    private void accumulateStats(InputSubSample input) {
        if (input.isEmpty()) {
            return;
        }

        sampleStat.accumulate(input);

        if (sampleStat.sampleSize() > 32) {
            Integer splitIndex = chooseIndexToSplitOn();
            splitOn(splitIndex);
        }
    }


    private void splitOn(int index) {
        splitIndex = Optional.of(index);

        leftChild = Optional.of(tree.nextChild());
        rightChild = Optional.of(tree.nextChild());
    }


    private Integer chooseIndexToSplitOn() {
        return Ordering.natural()
                .onResultOf((Map.Entry<Integer, Double> entry) -> entry.getValue() * Math.random())
                .max(sampleStat.entropies().entrySet())
                .getKey();
    }


    public void visitLeaves(Consumer<FeatureNode> visitor) {
        if (isLeaf()) {
            visitor.accept(this);
        } else {
            leftChild.get().visitLeaves(visitor);
            rightChild.get().visitLeaves(visitor);
        }
    }

    public int id() {
        return id;
    }
}
