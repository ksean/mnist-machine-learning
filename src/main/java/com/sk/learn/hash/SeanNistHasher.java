package com.sk.learn.hash;


import ao.ai.ml.model.input.RealList;
import com.sk.learn.NistInstance;

import java.util.List;

public enum SeanNistHasher
        implements SemanticHasher<NistInstance>
{
    INSTANCE;

    static final int THRESHOLD = 8;

    @Override
    public RealList hash(NistInstance hasher) {
        List<List<Integer>> inputColumns =
                null;



        return null;

//        return new RealList(
//                inputColumns
//                        .stream()
//                        .map(column ->
////                            column.stream().map()
//
//                                cell. < THRESHOLD
//                                        ? 0
//                                        : 1)
//                        .toArray());
    }

    @Override
    public String toString() {
        return "SeanNistHasher";
    }
}
