package com.sk.learn.gen.tree;


import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.clustering.*;

import java.util.*;

public class TreeFeatureLearner implements FeatureLearner
{
    private List<FeatureTree> trees;

    private Multiset<UUID> hitCounts;
    private Table<UUID, UUID, Integer> coActivations;
    private Map<UUID, Integer> clusters;
    private List<Integer> clusterSizes;


    public TreeFeatureLearner() {
        this(16 * 3);
    }

    public TreeFeatureLearner(int size) {
        trees = new ArrayList<>();

        hitCounts = HashMultiset.create();
        coActivations = HashBasedTable.create();
        clusters = new HashMap<>();
        clusterSizes = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            trees.add(new FeatureTree());
        }
    }


    @Override
    public void learn(InputSample sample) {
        Collection<UUID> leaves = new ArrayList<>(trees.size());
        for (FeatureTree tree : trees) {
            leaves.add(tree.learn(sample));
        }

        hitCounts.addAll(leaves);

        for (UUID i : leaves) {
            for (UUID j : leaves) {
                if (i.compareTo(j) >= 0) {
                    continue;
                }

                int existing = MoreObjects.firstNonNull(coActivations.get(i, j), 0);
                coActivations.put(i, j, existing + 1);
            }
        }

        clusters = new HashMap<>();
        clusterSizes = new ArrayList<>();
    }


    @Override
    public FeatureVector extract(InputSample input) {
        int featureCount = 64;

        if (clusters.isEmpty()) {
            cluster(featureCount);
        }

        int[] hits = new int[featureCount];
        for (FeatureTree tree : trees) {
            UUID activeLeaf = tree.matchingLeafId(input);

            int cluster = clusters.get(activeLeaf);
            hits[cluster]++;
        }

        boolean[] activations = new boolean[featureCount];
        for (int i = 0; i < featureCount; i++) {
            int clusterSize = clusterSizes.get(i);

            activations[i] =
                    hits[i] >= (clusterSize / 2);
        }

        return FeatureVector.create(activations);
    }


    private void cluster(int featureCount) {
        int nextIndex = 0;
        Map<UUID, Integer> leaveIndexes = new HashMap<>();
        for (FeatureTree tree : trees) {
            for (UUID leaf : tree.leafIds()) {
                leaveIndexes.put(leaf, nextIndex++);
            }
        }

        hitCounts.retainAll(leaveIndexes.keySet());
        coActivations.columnKeySet().retainAll(leaveIndexes.keySet());
        coActivations.rowKeySet().retainAll(leaveIndexes.keySet());

        final Table<UUID, UUID, Double> similarities = HashBasedTable.create();
        for (Table.Cell<UUID, UUID, Integer> c : coActivations.cellSet()) {
            int minHits = Math.min(hitCounts.count(c.getRowKey()), hitCounts.count(c.getColumnKey()));
            double similarity = (double) c.getValue() / minHits;
            similarities.put(c.getRowKey(), c.getColumnKey(), accountForStatisticalError(similarity, minHits));
        }

        double[][] similarityMatrix = new double[leaveIndexes.size()][leaveIndexes.size()];
        for (Table.Cell<UUID, UUID, Double> similarity : similarities.cellSet()) {
            int rowIndex = leaveIndexes.get(similarity.getRowKey());
            int columnIndex = leaveIndexes.get(similarity.getColumnKey());
            similarityMatrix[rowIndex][columnIndex] = similarity.getValue();
            similarityMatrix[columnIndex][rowIndex] = similarity.getValue();
        }
        for (int i = 0; i < leaveIndexes.size(); i++) {
            similarityMatrix[i][i] = 1;
        }

        double[][] normalizerMatrix = new double[leaveIndexes.size()][leaveIndexes.size()];
        for (int i = 0; i < leaveIndexes.size(); i++) {
            double sum = 0;
            for (int j = 0; j < leaveIndexes.size(); j++) {
                sum += similarityMatrix[i][j];
            }
            normalizerMatrix[i][i] = sum;
        }

        RealMatrix sim = MatrixUtils.createRealMatrix(similarityMatrix);
        RealMatrix norm = MatrixUtils.createRealMatrix(normalizerMatrix);

        RealMatrix normInverse = new LUDecomposition(norm).getSolver().getInverse();
        RealMatrix randomWalkNormalizedLaplacian = normInverse.multiply(sim);

        EigenDecomposition eigen;// = new EigenDecomposition(randomWalkNormalizedLaplacian);
        for (int i = 0;;) {
            Random random = new Random();

            try {
                eigen = new EigenDecomposition(randomWalkNormalizedLaplacian);
                break;
            } catch (MaxCountExceededException ignored) {
                randomWalkNormalizedLaplacian.setEntry(
                        random.nextInt(leaveIndexes.size()),
                        random.nextInt(leaveIndexes.size()),
                        random.nextDouble() - 0.5);

                System.out.println("trying eingen decomp " + i++);
                if (i >= 100) {
                    throw ignored;
                }
            }
        }

        class FeatureLeaf extends DoublePoint {
            public final UUID leaf;
            public FeatureLeaf(double[] point, UUID leaf) {
                super(point);
                this.leaf = leaf;
            }

            @Override
            public String toString() {
                return leaf + "|" + super.toString();
            }
        }

        List<FeatureLeaf> points = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : leaveIndexes.entrySet()) {
            int index = e.getValue();
            double[] component = new double[featureCount];
            for (int k = 0; k < featureCount; k++) {
                component[k] = eigen.getEigenvector(k).getEntry(index);
            }
            points.add(new FeatureLeaf(component, e.getKey()));
        }

        Clusterer<FeatureLeaf> clusterer = new MultiKMeansPlusPlusClusterer<>(
                new KMeansPlusPlusClusterer<>(featureCount, 32_000), 128);
        List<? extends Cluster<FeatureLeaf>> centroids = clusterer.cluster(points);
        for (int i = 0; i < centroids.size(); i++) {
            Cluster<FeatureLeaf> centroid = centroids.get(i);
            System.out.println("Cluster " + i + " has " + centroid.getPoints().size());

            clusterSizes.add(centroid.getPoints().size());
            for (FeatureLeaf l : centroid.getPoints()) {
                clusters.put(l.leaf, i);
            }
        }
    }



    public static double accountForStatisticalError(
            double quantity,
            int    dataPointCount)
    {
        double statisticalErrorPercent =
                Math.sqrt(dataPointCount) / (dataPointCount + 1);

        double statisticalError =
                statisticalErrorPercent * quantity;

        return quantity - statisticalError;
    }
}
