package com.sk.learn.gen.tree;

import com.google.common.collect.ImmutableList;
import com.sk.learn.domain.InputSample;

import java.util.Optional;
import java.util.UUID;

public class FeatureTree
{
    private FeatureNode root;

    private Optional<ImmutableList<FeatureNode>> leavesCache = Optional.empty();


    public FeatureTree() {
        root = new FeatureNode();
    }

    public void learn(InputSample sample) {
        leavesCache = Optional.empty();
        root.learn(InputSubSample.create(sample));
    }

    public int matchingLeafIndex(InputSample sample) {
        UUID matchingLeafId = root.identifyLeaf(InputSubSample.create(sample));

        int index = 0;
        for (FeatureNode leaf : leaves()) {
            if (leaf.id().equals(matchingLeafId)) {
                return index;
            }

            index++;
        }

        throw new Error();
    }


    public int leafCount() {
        return leaves().size();
    }

    public ImmutableList<FeatureNode> leaves() {
        if (leavesCache.isPresent()) {
            return leavesCache.get();
        }

        ImmutableList.Builder<FeatureNode> buffer = ImmutableList.builder();
        root.visitLeaves(buffer::add);

        ImmutableList<FeatureNode> leaves = buffer.build();
        leavesCache = Optional.of(leaves);
        return leaves;
    }
}
