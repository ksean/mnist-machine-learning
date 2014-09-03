package com.sk.learn;

import ao.ai.ml.model.input.RealList;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.List;


@AutoValue
public abstract class NistInstance
{
    public static NistInstance create(
            List<Integer> inputRow,
            Table<Integer, Integer, Integer> input,
            Integer output) {
        return new AutoValue_NistInstance(
                ImmutableList.copyOf(inputRow),
                ImmutableTable.copyOf(input),
                Preconditions.checkNotNull(output));
    }

    public abstract List<Integer> inputRow();

    public abstract Table<Integer, Integer, Integer> input();

    public abstract Integer output();


//    public List<List<Integer>> inputMatrix() {
//        input().rowMap().values();
//
//        .map(row );
//
//    }

    public RealList inputRealList() {
        return new RealList(
                inputRow().stream().mapToDouble(
                        value -> (double) value
                ).toArray());
    }
}