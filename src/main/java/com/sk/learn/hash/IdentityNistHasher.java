package com.sk.learn.hash;

import ao.ai.ml.model.input.RealList;
import com.sk.learn.NistInstance;

public enum IdentityNistHasher
        implements SemanticHasher<NistInstance>
{
    INSTANCE;

    @Override
    public RealList hash(NistInstance hasher) {
        return hasher.inputRealList();
    }

    @Override
    public String toString() {
        return "IdentityNistHasher";
    }
}
