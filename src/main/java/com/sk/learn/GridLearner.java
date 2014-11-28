package com.sk.learn;


import ao.ai.classify.online.forest.OnlineRandomForest;
import ao.ai.ml.model.algo.OnlineMultiLearner;
import ao.ai.ml.model.input.RealList;
import ao.ai.ml.model.output.MultiClass;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class GridLearner
{
    private Table<Integer, Integer, OnlineMultiLearner<RealList>> learners =
            HashBasedTable.create();


    public void learn(RealList semanticHash, Table<Integer, Integer, Integer> input)
    {
        for (Table.Cell<Integer, Integer, Integer> c : input.cellSet())
        {
            OnlineMultiLearner<RealList> cellLearner;
            if (learners.contains(c.getRowKey(), c.getColumnKey())) {
                cellLearner = learners.get(c.getRowKey(), c.getColumnKey());
            } else {
                cellLearner = new OnlineRandomForest(
                        32
//                        ,
//                        new Param(
//                            2,
//                            10,
//                            (int) Math.ceil(
//                                    Math.sqrt( input.size() )),
//                            0)
                );
                learners.put(c.getRowKey(), c.getColumnKey(), cellLearner);
            }

            cellLearner.learn(semanticHash, MultiClass.create(c.getValue()));
        }
    }

    public Table<Integer, Integer, Integer> predict(RealList semanticHash) {
        ImmutableTable.Builder<Integer, Integer, Integer> gridPrediction = ImmutableTable.builder();

        for (Table.Cell<Integer, Integer, OnlineMultiLearner<RealList>> learner : learners.cellSet()) {
            int prediction = learner.getValue().classify(semanticHash).best();

            gridPrediction.put(learner.getRowKey(), learner.getColumnKey(), prediction);
        }

        return gridPrediction.build();
    }
}
