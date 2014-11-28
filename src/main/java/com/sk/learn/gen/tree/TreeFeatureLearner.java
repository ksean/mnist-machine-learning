package com.sk.learn.gen.tree;


import ao.graph.Graph;
import ao.graph.impl.common.SimpleAbsDomain;
import ao.graph.impl.fast.BufferedFastGraph;
import ao.graph.struct.DataAndWeight;
import ao.graph.struct.Endpoints;
import ao.graph.struct.NodeDataPair;
import ao.graph.user.EdgeWeight;
import ao.prophet.impl.cluster.Cluster;
import ao.prophet.impl.cluster.InternalCluster;
import ao.prophet.impl.cluster.LeafCluster;
import ao.util.math.stats.Stats;
import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import com.sk.learn.domain.FeatureVector;
import com.sk.learn.domain.InputSample;
import com.sk.learn.gen.FeatureLearner;

import java.util.*;

public class TreeFeatureLearner implements FeatureLearner
{
    private List<FeatureTree> trees;

    private Multiset<UUID> hitCounts;
    private Table<UUID, UUID, Integer> coActivations;
    private Optional<Cluster<UUID>> clusters;


    public TreeFeatureLearner() {
        this(64);
    }

    public TreeFeatureLearner(int size) {
        trees = new ArrayList<>();

        hitCounts = HashMultiset.create();
        coActivations = HashBasedTable.create();
        clusters = Optional.empty();
        
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

        clusters = Optional.empty();
    }


    @Override
    public FeatureVector extract(InputSample input) {
        if (! clusters.isPresent()) {
            Collection<UUID> leaves = new ArrayList<>();
            for (FeatureTree tree : trees) {
                leaves.addAll(tree.leafIds());
            }

            hitCounts.retainAll(leaves);
            coActivations.columnKeySet().retainAll(leaves);
            coActivations.rowKeySet().retainAll(leaves);

            final Table<UUID, UUID, Double> similarities = HashBasedTable.create();
            for (Table.Cell<UUID, UUID, Integer> c : coActivations.cellSet()) {
                int minHits = Math.min(hitCounts.count(c.getRowKey()), hitCounts.count(c.getColumnKey()));
                double similarity = (double) c.getValue() / minHits;
                similarities.put(c.getRowKey(), c.getColumnKey(), Stats.accountForStatisticalError(similarity, minHits));
//                similarities.put(c.getRowKey(), c.getColumnKey(), similarity);
            }


            class ComponentEdgeWeight implements EdgeWeight<ComponentEdgeWeight> {
                private Set<UUID> left = new HashSet<>();
                private Set<UUID> right = new HashSet<>();

                public ComponentEdgeWeight() {}
                public ComponentEdgeWeight(UUID leftLeaf, UUID rightLeaf) {
                    left.add(leftLeaf);
                    right.add(rightLeaf);
                }

                @Override
                public ComponentEdgeWeight mergeWith(ComponentEdgeWeight componentEdgeWeight) {
                    if (componentEdgeWeight.left.isEmpty() && componentEdgeWeight.right.isEmpty()) {
                        return this;
                    } else if (left.isEmpty() && right.isEmpty()) {
                        return componentEdgeWeight;
                    }

//                    if (left.isEmpty() || right.isEmpty() || componentEdgeWeight.left.isEmpty() || componentEdgeWeight.right.isEmpty()) {
//                        System.out.println("foo");
//                    }

//                    if (! (left.isEmpty() || right.isEmpty() || componentEdgeWeight.left.isEmpty() || componentEdgeWeight.right.isEmpty())) {
//                        System.out.println("foo");
//                    }

                    ComponentEdgeWeight merged = new ComponentEdgeWeight();

                    merged.left.addAll(left);
                    merged.right.addAll(right);

                    if (! Sets.intersection(left, componentEdgeWeight.left).isEmpty() || ! Sets.intersection(right, componentEdgeWeight.right).isEmpty()) {
                        merged.left.addAll(componentEdgeWeight.left);
                        merged.right.addAll(componentEdgeWeight.right);
                    } else if (! Sets.intersection(left, componentEdgeWeight.right).isEmpty() || ! Sets.intersection(right, componentEdgeWeight.left).isEmpty()) {
                        merged.left.addAll(componentEdgeWeight.right);
                        merged.right.addAll(componentEdgeWeight.left);
                    } else {
                        System.out.println("wha?");
                        // arbitrary ?
                        merged.left.addAll(componentEdgeWeight.left);
                        merged.right.addAll(componentEdgeWeight.right);
                    }

                    if (! Sets.intersection(merged.left, merged.right).isEmpty()) {
                        System.out.println("wow!");
                    }

                    return merged;
                }

                private double compare(UUID a, UUID b) {
                    UUID lo = Ordering.natural().min(a, b);
                    UUID hi = Ordering.natural().max(a, b);
                    Double sim = similarities.get(lo, hi);
                    return MoreObjects.firstNonNull(sim, 0.0);
                }

                @Override
                public float asFloat() {
                    if (left.isEmpty() || right.isEmpty()) {
                        System.out.println("bar");
                    }

                    double sum = 0;
                    int count = 0;

                    for (UUID l : left) {
                        for (UUID r : right) {
                            sum += compare(l, r);
                            count++;
                        }
                    }

                    return (float) (sum / count);
                }

                @Override
                public String toString() {
                    return String.valueOf(asFloat());
                }
            }


            Graph<Cluster<UUID>, ComponentEdgeWeight> graph = new BufferedFastGraph<>(
                    new SimpleAbsDomain<>(1024),
                    new ComponentEdgeWeight());


            Map<UUID, LeafCluster<UUID>> leafClusters = new HashMap<>();
            for (UUID leaf : leaves) {
                LeafCluster<UUID> leafCluster = new LeafCluster<>(leaf);
                graph.add(leafCluster);
                leafClusters.put(leaf, leafCluster);
            }

            for (Table.Cell<UUID, UUID, Integer> c : coActivations.cellSet()) {
                graph.join(
                        leafClusters.get(c.getRowKey()),
                        leafClusters.get(c.getColumnKey()),
                        new ComponentEdgeWeight(c.getRowKey(), c.getColumnKey()));
            }

            Cluster<UUID> root = agglomerativeClusterAnalysis(graph);
            System.out.println(root);
            clusters = Optional.of(root);
        }


        List<FeatureVector> vectors = new ArrayList<>();

        for (FeatureTree tree : trees) {
            int trueIndex = tree.matchingLeafIndex(input);
            FeatureVector vector = FeatureVector.createSingleFeature(tree.leafCount(), trueIndex);
            vectors.add(vector);
        }

//        int featureCount = vectors.stream().mapToInt(FeatureVector::size).sum();
//        boolean[] activations = new boolean[featureCount];

        int size = 8;
        int[] hits = new int[size];
//        int[] misses = new int[size];

        int index = 0;
        for (FeatureVector vector : vectors) {
            for (int i = 0; i < vector.size(); i++) {
                //activations[index] = vector.get(i);

                int modIndex = index % size;
                if (vector.get(i)) {
                    hits[modIndex]++;
                }/* else {
                    misses[modIndex]++;
                }*/

                index++;
            }
        }

        int threshold = trees.size() / size;
        boolean[] activations = new boolean[size];
        for (int i = 0; i < size; i++) {
            activations[i] = hits[i] > threshold;
        }

        return FeatureVector.create(activations);
    }


    private <I, W extends EdgeWeight<W>> Cluster<I> agglomerativeClusterAnalysis(
            Graph<Cluster<I>, W> graph)
    {
        //int n = 0;
        InternalCluster<I> root = null;
        while (true)
        {
            //System.out.println("n = " + (n++));
            Endpoints<Cluster<I>, W> mostRelatedClusters =
                    graph.nodesIncidentHeaviestEdge();

            NodeDataPair<Cluster<I>> toMerge;
            if (mostRelatedClusters == null)
            {
                toMerge = graph.antiEdge();

                if (toMerge == null && root == null)
                {
                    return null;
                }
            }
            else
            {
//                // not sure if this ever made much sense
//                if (mostRelatedClusters.weight().isLighterThanUnrelated())
//                {
//                    toMerge = graph.antiEdge();
//                    if (toMerge == null)
//                    {
//                        toMerge = mostRelatedClusters.nodes();
//                    }
//                }
//                else
//                {
                    toMerge = mostRelatedClusters.nodes();
//                }
            }

            if (toMerge == null) break;

            DataAndWeight<Cluster<I>, W> merged =
                    graph.merge( toMerge.dataA(), toMerge.dataB() );

            root = (InternalCluster<I>) merged.data();
//            root.relationBetweenChildren( merged.weight() );
        }
        return root;
    }
}
