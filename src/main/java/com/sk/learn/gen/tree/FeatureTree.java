package com.sk.learn.gen.tree;

import com.google.common.collect.ImmutableList;
import com.sk.learn.domain.InputSample;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FeatureTree
{
    private FeatureNode root;

    private Optional<ImmutableList<FeatureNode>> leavesCache = Optional.empty();


    public FeatureTree() {
        root = new FeatureNode();
    }

    public UUID learn(InputSample sample) {
        leavesCache = Optional.empty();
        return root.learn(InputSubSample.create(sample));
    }

    public UUID matchingLeafId(InputSample sample) {
        return root.identifyLeaf(InputSubSample.create(sample));
    }

    public int indexOfLeaf(UUID id) {
        int index = 0;
        for (FeatureNode leaf : leaves()) {
            if (leaf.id().equals(id)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    public int matchingLeafIndex(InputSample sample) {
        UUID matchingLeafId = root.identifyLeaf(InputSubSample.create(sample));
        return indexOfLeaf(matchingLeafId);
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

    public Collection<UUID> leafIds() {
        return leaves().stream().map(FeatureNode::id).collect(Collectors.toList());
    }
}
