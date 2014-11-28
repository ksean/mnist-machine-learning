package com.sk.learn;

import ao.ai.ml.model.input.RealList;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.ArrayList;
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

    public static NistInstance create(boolean[][] grid, int labelValue) {
        List<Integer> inputRow = new ArrayList<>();
        for (boolean[] row : grid) {
            for (boolean cell : row) {
                inputRow.add(fromBoolean(cell));
            }
        }

        Table<Integer, Integer, Integer> input = HashBasedTable.create();
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                input.put(row, col, fromBoolean(grid[row][col]));
            }
        }

        return create(inputRow, input, labelValue);
    }

    private static int fromBoolean(boolean cell) {
        return cell ? 1 : 0;
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