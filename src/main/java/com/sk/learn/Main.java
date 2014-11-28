package com.sk.learn;


import ao.ai.classify.online.forest.OnlineRandomForest;
import ao.ai.ml.model.algo.OnlineMultiLearner;
import ao.ai.ml.model.input.RealList;
import ao.ai.ml.model.output.MultiClass;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.sk.learn.domain.BooleanMeasurement;
import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;
import com.sk.learn.gen.tree.TreeFeatureLearner;
import com.sk.learn.hash.BinaryNistHasher;
import com.sk.learn.hash.SemanticHasher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main
{
    static final String DELIMITER = ",";
    static final Splitter PARSER = Splitter.on(DELIMITER);
    static final Joiner FORMATTER = Joiner.on(DELIMITER);
    static final int COLUMNS = 8;


    public static void main(String[] args) throws IOException {
        CharSource input = Files.asCharSource(
//                new File("D:/Downloads/optdigits.tra"),
//                new File("C:/~/data/optdigits.tra"),
                new File("C:/~/data/optdigits-orig.tra"),
                Charsets.UTF_8);

        List<NistInstance> instances =
//                readInstances(input);
                readOrigInstances(input);

        FeatureLearner featureLearner = new TreeFeatureLearner();

        for (NistInstance instance : instances) {
            featureLearner.learn(toInputSample(instance));
        }

        GridLearner gridLearner = new GridLearner();
//        int learnedFeature = 0;

        int learnIndex = 0;
        for (NistInstance instance : instances) {
            FeatureVector featureVector = featureLearner.extract(toInputSample(instance));
            RealList learningSample = toRealList(featureVector);
            gridLearner.learn(learningSample, instance.input());

            if (learnIndex++ % 100 == 0) {
                System.out.println("Learned: " + learnIndex);
            }
        }

        File outputPath = new File(String.format("out/feature_%s.csv", System.currentTimeMillis()));
        Files.createParentDirs(outputPath);

        CharSink output = Files.asCharSink(
                outputPath, Charsets.UTF_8);

        try (PrintWriter out = new PrintWriter(output.openBufferedStream()))
        {
//            Table<Integer, Integer, Integer> falsePredicted = gridLearner.predict(toRealList(false));
//            write(out, falsePredicted);
//
//            out.println();
//
//            Table<Integer, Integer, Integer> truePredicted = gridLearner.predict(toRealList(true));
//            write(out, truePredicted);

            long count = 0;
            for (NistInstance instance : instances) {
                FeatureVector featureVector = featureLearner.extract(toInputSample(instance));
                RealList learningSample = toRealList(featureVector);

                out.println(count);
                write(out, instance.input());
                out.println("\n");

                Table<Integer, Integer, Integer> predicted = gridLearner.predict(learningSample);
                write(out, predicted);

                out.println("\n\n\n");

                if (count++ % 100 == 0) {
                    System.out.println("Displayed: " + count);
                }
            }
        }

        //learnInstances(instances);
    }


    private static InputSample toInputSample(NistInstance instance) {
        RealList pixels = BinaryNistHasher.INSTANCE.hash(instance);

        BooleanMeasurement[] measurements = new BooleanMeasurement[pixels.size()];

        for (int i = 0; i < measurements.length; i++) {
            boolean isSet = pixels.get(i) == 0;
            measurements[i] = BooleanMeasurement.create(isSet);
        }

        return InputSample.create(measurements);
    }


    private static RealList toRealList(FeatureVector features) {
        double[] values = new double[features.size()];
        for (int i = 0; i < features.size(); i++) {
            values[i] = (features.get(i) ? 1 : 0);
        }
        return new RealList(values);
    }

    private static final Pattern label = Pattern.compile("^ (\\d)$");
    private static final Pattern row = Pattern.compile("^([0|1]{32})$");

    private static List<NistInstance> readOrigInstances(CharSource input) throws IOException {
        int labelValue;
        List<NistInstance> instances = new ArrayList<>();

        int nextGridRow = 0;
        boolean[][] grid = new boolean[32][32];

        for (String line : input.readLines()) {
            Matcher lineMatcher = label.matcher(line);
            if (lineMatcher.find()) {
                labelValue = Integer.valueOf(lineMatcher.group(1));

                instances.add(
                        NistInstance.create(grid, labelValue));

                nextGridRow = 0;
                continue;
            }

            Matcher rowMatcher = row.matcher(line);
            if (rowMatcher.find()) {
                String rowValue = rowMatcher.group(1);
                for (int i = 0; i < 32; i++) {
                    grid[nextGridRow][i] = (rowValue.charAt(i) == '1');
                }

                nextGridRow++;
            }

        }

        return instances;
    }

    private static List<NistInstance> readInstances(CharSource input) throws IOException {
        return input.readLines().stream().map(line -> {
            List<String> tokens = PARSER.splitToList(line);
            List<Integer> values = tokens.stream().map(Integer::parseInt).collect(Collectors.toList());

            List<Integer> instanceInput = values.subList(0, values.size() - 1);
            int instanceOutput = values.get(values.size() - 1);

            ImmutableTable.Builder<Integer, Integer, Integer> instance = ImmutableTable.builder();
            for (int i = 0; i < instanceInput.size(); i++) {
                int value = instanceInput.get(i);
                int row = i / COLUMNS;
                int column = i % COLUMNS;

                instance.put(row, column, value);
            }

            return NistInstance.create(
                    instanceInput,
                    instance
                            .orderRowsBy(Comparator.<Integer>naturalOrder())
                            .orderColumnsBy(Comparator.<Integer>naturalOrder())
                            .build(),
                    instanceOutput);
        }).collect(Collectors.toList());
    }

    private static void learnInstances(List<NistInstance> instances) throws IOException {
        SemanticHasher<NistInstance> hasher =
//                IdentityNistHasher.INSTANCE;
                BinaryNistHasher.INSTANCE;

        File outputPath = new File(String.format("out/nist_%s.csv", hasher));
        Files.createParentDirs(outputPath);

        CharSink output = Files.asCharSink(
                outputPath, Charsets.UTF_8);

        GridLearner gridLearner = new GridLearner();

        AtomicLong count = new AtomicLong();
        try (PrintWriter out = new PrintWriter(output.openBufferedStream()))
        {
//            List<Table<Integer, Integer, Integer>> grids =
//                    StreamSupport.stream(instances.spliterator(), false)
////                    .map(NistInstance::input)
//                    .collect(Collectors.toList());

            OnlineMultiLearner<RealList> learner = new OnlineRandomForest();

            instances.forEach(instance -> {
                out.println("\n\n");

                RealList inputValues = instance.inputRealList();

                MultiClass prediction = learner.classify(inputValues);

                boolean correct =
                        (prediction.best() == instance.output());

                out.println("INFO," + prediction.best());
                out.println("INFO," + instance.output());
                out.println("INFO," + correct);

                learner.learn(inputValues, MultiClass.create(instance.output()));

                RealList semanticHash = hasher.hash(instance);

                gridLearner.learn(semanticHash, instance.input());

                write(out, instance.input());

                out.println();

                write(out, gridLearner.predict(semanticHash));

                if (count.incrementAndGet() % 100 == 0) {
                    System.out.println(count.longValue() + "\t" + LocalDateTime.now());
                }
            });
        }
    }

    private static void write(PrintWriter out, Table<Integer, Integer, Integer> grid) {
        for (Map<Integer, Integer> row : grid.rowMap().values()) {
            String rowOutput = FORMATTER.join(row.values());
            out.println(rowOutput);
        }
    }
}
