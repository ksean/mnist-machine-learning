package com.sk.learn.gen.tree;


import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.clustering.*;

import java.util.*;

public class TreeFeatureLearner implements FeatureLearner
{
    private static final double eigenDeltaThreshold = 0.0000001;

    private List<FeatureTree> trees;

    private Multiset<UUID> hitCounts;
    private Table<UUID, UUID, Integer> coActivations;
    private Map<UUID, Integer> clusters;
    private List<Integer> clusterSizes;


    public TreeFeatureLearner() {
        this(16 * 3 + 1);
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
        int featureCount = 10;

        if (clusters.isEmpty()) {
            cluster(featureCount);
        }

        int[] hits = new int[featureCount];
        for (FeatureTree tree : trees) {
            UUID activeLeaf = tree.matchingLeafId(input);

            int cluster = clusters.get(activeLeaf);
            hits[cluster]++;
        }

        double activationProbability =
                (double) trees.size() / clusters.size();

        boolean[] activations = new boolean[featureCount];
        for (int i = 0; i < featureCount; i++) {
            int clusterSize = clusterSizes.get(i);
            double threshold = clusterSize * activationProbability;

            activations[i] =
//                    hits[i] >= (clusterSize / 2);
//                hits[i] > 0;
                hits[i] >= threshold * 1.1;
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

//            normalizerMatrix[i][i] = sum;
            normalizerMatrix[i][i] = Math.pow(sum, -0.5);
        }

        RealMatrix sim = MatrixUtils.createRealMatrix(similarityMatrix);
        RealMatrix norm = MatrixUtils.createRealMatrix(normalizerMatrix);

//        RealMatrix normInverse = new LUDecomposition(norm).getSolver().getInverse();
//        RealMatrix normalizedLaplacian = normInverse.multiply(sim);

        RealMatrix normalizedLaplacian = norm.multiply(sim).multiply(norm);


        EigenDecomposition eigen;// = new EigenDecomposition(randomWalkNormalizedLaplacian);
        for (int i = 0;;) {
            Random random = new Random();

            try {
                eigen = new EigenDecomposition(normalizedLaplacian);
                break;
            } catch (MaxCountExceededException ignored) {
                int index = random.nextInt(leaveIndexes.size());
                normalizedLaplacian.setEntry(
                        index, index, 1);

                System.out.println("trying eingen decomp " + i++);
                if (i >= 100) {
                    throw ignored;
                }
            }
        }

        int nextEigenIndex = 0;
        SortedMap<Double, Integer> eigenValueIndexes = new TreeMap<>(Ordering.<Double>natural().reverse());
        for (double eigenValue : eigen.getRealEigenvalues()) {
            eigenValueIndexes.put(eigenValue, nextEigenIndex++);
        }

        double previousEigenValue = Double.NaN;
        List<Integer> largestEigenValueIndexes = new ArrayList<>();
        for (Map.Entry<Double, Integer> eigenValueIndex : eigenValueIndexes.entrySet()) {
            if (Double.isNaN(previousEigenValue) || Math.abs(previousEigenValue - eigenValueIndex.getKey()) > eigenDeltaThreshold) {
                largestEigenValueIndexes.add(eigenValueIndex.getValue());

                if (largestEigenValueIndexes.size() >= featureCount) {
                    break;
                }
            }

            previousEigenValue = eigenValueIndex.getKey();
        }

        List<FeatureLeaf> points = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : leaveIndexes.entrySet()) {
            int index = e.getValue();

            double sumOfSquares = 0;
            double[] component = new double[featureCount];
            for (int k = 0; k < featureCount; k++) {
                int eigenVectorIndex = largestEigenValueIndexes.get(k);
                component[k] = eigen.getEigenvector(eigenVectorIndex).getEntry(index);
                sumOfSquares += Math.pow(component[k], 2);
            }

            double normalizer = Math.sqrt(sumOfSquares);

            for (int k = 0; k < featureCount; k++) {
                component[k] /= normalizer;
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


    private static class FeatureLeaf extends DoublePoint {
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
}
