package ar.edu.itba.sds.tp2.observable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ClusterAnalyzer {

    private ClusterAnalyzer() {
    }

    public static int largestClusterSize(Map<Integer, Set<Integer>> neighbours) {
        if (neighbours.isEmpty()) {
            return 0;
        }
        UnionFind unionFind = new UnionFind(neighbours.keySet());
        for (Map.Entry<Integer, Set<Integer>> entry : neighbours.entrySet()) {
            int id = entry.getKey();
            for (int neighbourId : entry.getValue()) {
                unionFind.union(id, neighbourId);
            }
        }
        return unionFind.largestComponentSize();
    }

    public static double giantComponentFraction(Map<Integer, Set<Integer>> neighbours) {
        if (neighbours.isEmpty()) {
            return 0.0;
        }
        return (double) largestClusterSize(neighbours) / neighbours.size();
    }

    private static final class UnionFind {
        private final Map<Integer, Integer> parent = new HashMap<>();
        private final Map<Integer, Integer> componentSize = new HashMap<>();

        UnionFind(Set<Integer> ids) {
            for (int id : ids) {
                parent.put(id, id);
                componentSize.put(id, 1);
            }
        }

        int find(int id) {
            int root = id;
            while (parent.get(root) != root) {
                root = parent.get(root);
            }
            int current = id;
            while (parent.get(current) != root) {
                int next = parent.get(current);
                parent.put(current, root);
                current = next;
            }
            return root;
        }

        void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA == rootB) {
                return;
            }
            if (componentSize.get(rootA) < componentSize.get(rootB)) {
                int tmp = rootA;
                rootA = rootB;
                rootB = tmp;
            }
            parent.put(rootB, rootA);
            componentSize.put(rootA, componentSize.get(rootA) + componentSize.get(rootB));
        }

        int largestComponentSize() {
            int max = 0;
            for (int id : parent.keySet()) {
                if (find(id) == id) {
                    max = Math.max(max, componentSize.get(id));
                }
            }
            return max;
        }
    }
}
