package com.sk.learn.gen.tree;

import com.google.common.collect.Ordering;
import com.sk.learn.domain.BooleanMeasurement;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FeatureNode {
    private final UUID id;

    private SubSampleStat sampleStat = new SubSampleStat();

    private Optional<Integer> splitIndex = Optional.empty();
    private Optional<FeatureNode> leftChild = Optional.empty();
    private Optional<FeatureNode> rightChild = Optional.empty();


    public FeatureNode() {
        this.id = UUID.randomUUID();
    }

    public UUID identifyLeaf(InputSubSample input) {
        if (isLeaf()) {
            return id;
        }

        return matchingChild(input).identifyLeaf(input);
    }


    public UUID learn(InputSubSample input) {
        if (isLeaf()) {
            accumulateStats(input);
            return id;
        } else {
            return learnInChildNodes(input);
        }
    }

    private boolean isLeaf() {
        return ! splitIndex.isPresent();
    }


    private UUID learnInChildNodes(InputSubSample input) {
        FeatureNode matchingChild = matchingChild(input);

        InputSubSample remainingInput = input.remove(splitIndex.get());

        return matchingChild.learn(remainingInput);
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

        if (sampleStat.sampleSize() > 250) {
            Integer splitIndex = chooseIndexToSplitOn();
            splitOn(splitIndex);
        }
    }


    private void splitOn(int index) {
        splitIndex = Optional.of(index);

        leftChild = Optional.of(new FeatureNode());
        rightChild = Optional.of(new FeatureNode());
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

    public UUID id() {
        return id;
    }
}
