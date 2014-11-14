package com.sk.learn.hash;

import ao.ai.ml.model.input.RealList;
import com.sk.learn.NistInstance;

public enum BinaryNistHasher
        implements SemanticHasher<NistInstance>
{
    INSTANCE;

    static final int THRESHOLD = 8;

    @Override
    public RealList hash(NistInstance hasher) {
        return new RealList(
            hasher
                .input()
                .cellSet()
                .stream()
                .mapToDouble(cell ->
                        cell.getValue() < THRESHOLD
                        ? 0
                        : 1)
                .toArray());
    }

    @Override
    public String toString() {
        return "BinaryNistHasher";
    }
}
