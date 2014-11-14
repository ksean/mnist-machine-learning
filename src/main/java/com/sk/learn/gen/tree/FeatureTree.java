package com.sk.learn.gen.tree;

import com.google.common.collect.ImmutableList;
import com.sk.learn.domain.InputSample;

import java.util.function.Consumer;

public class FeatureTree
{
    private int nextId;
    private FeatureNode root;


    public FeatureTree() {
        root = nextChild();
    }

    public void learn(InputSample sample) {
        root.learn(InputSubSample.create(sample));
    }

    public int matchingLeafIndex(InputSample sample) {
        int matchingLeafId = root.identifyLeaf(InputSubSample.create(sample));

        int index = 0;
        for (FeatureNode leaf : leaves()) {
            if (leaf.id() == matchingLeafId) {
                return index;
            }

            index++;
        }

        throw new Error();
    }

//
//    public int leafCount() {
//        return leaves().size();
//    }

    public ImmutableList<FeatureNode> leaves() {
        ImmutableList.Builder<FeatureNode> leaves = ImmutableList.builder();
        root.visitLeaves(leaves::add);
        return leaves.build();
    }

    public FeatureNode nextChild() {
        return new FeatureNode(this, nextId++);
    }
}
