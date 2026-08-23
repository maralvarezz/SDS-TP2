package ar.edu.itba.sds.tp2.observable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Identifica clusters sobre el grafo de vecinos que ya devolvio el CIM, usando Union-Find. Un
 * cluster es un conjunto de particulas donde todo par esta conectado por una cadena de saltos
 * vecino-a-vecino dentro de rc, tal como lo define el enunciado (punto d). No vuelve a calcular
 * distancias ni corre el CIM de nuevo -- toma el mismo Map(id, vecinos) que ya se uso para la
 * regla de direccion en ese paso.
 */
public final class ClusterAnalyzer {

    private ClusterAnalyzer() {
    }

    /**
     * Tamaño (en cantidad de particulas) del cluster mas grande de la red.
     */
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

    /**
     * Fraccion de particulas que forman parte del cluster mas grande (componente gigante S,
     * punto d del enunciado): S = tamaño del cluster mas grande / N.
     */
    public static double giantComponentFraction(Map<Integer, Set<Integer>> neighbours) {
        if (neighbours.isEmpty()) {
            return 0.0;
        }
        return (double) largestClusterSize(neighbours) / neighbours.size();
    }

    /**
     * Union-Find (union por tamaño + compresion de camino) sobre los ids de particula. Practicamente
     * O(N) para la cantidad de uniones que aparecen en una corrida de este TP.
     */
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
