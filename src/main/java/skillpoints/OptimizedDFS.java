package skillpoints;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @author d0cr
 */
public class OptimizedDFS {
    static int[][] reqs, deltas;
    static int[] startStats;
    static int n, s;

    static final int TRIAL_COUNT = 100;

    public static void main(String[] args) {
        Random random = new Random();

        int solved = 0, infeasible = 0;
        long totalElapsed = 0;

        for (int trial = 1; trial <= TRIAL_COUNT; trial++) {
            int[][] trialReqs = IntStream.range(0, 8)
                    .mapToObj(i -> IntStream.generate(() -> random.nextInt(20) - 10)
                            .map(x -> x == 0 ? Integer.MIN_VALUE : x).limit(5).toArray())
                    .toArray(int[][]::new);

            int[][] trialDeltas = IntStream.range(0, 8)
                    .mapToObj(i -> IntStream.generate(() -> random.nextInt(20) - 10)
                            .limit(5).toArray())
                    .toArray(int[][]::new);

            int[] trialStartStats = IntStream.range(0, 5).map(i -> random.nextInt(50)).toArray();

            trialReqs = new int[][]{{1, 0, 0, 0, 0}, {0, 2, 0, 0, 0}, {1, 0, 0, 0, 0}};
            trialDeltas = new int[][]{{-1, 2, 0, 0, 0}, {1, 0, 0, 0, 0}, {0, 0, 1, 0, 0}};
            trialStartStats = new int[]{2, 0, 0, 0, 0};

            if (trial == 1) solve(trialReqs.clone(), trialDeltas.clone(), trialStartStats.clone());

            long start = System.nanoTime();
            int[] result = solve(trialReqs, trialDeltas, trialStartStats);
            long elapsed = System.nanoTime() - start;

            totalElapsed += elapsed;

            System.out.printf("Trial %3d/%d, Solved in %.3fms, ", trial, TRIAL_COUNT, elapsed / 1e6);
            System.out.println("Order: " + Arrays.toString(result));
            printOrder(result);
            System.out.println();
        }

        System.out.println("=".repeat(60));
        System.out.printf("Trials: %d | Solved: %d | Infeasible: %d%n",
                TRIAL_COUNT, solved, infeasible);
        System.out.printf("Total time: %.3f ms | Avg per trial: %.3f ms%n",
                totalElapsed / 1e6, totalElapsed / 1e6 / TRIAL_COUNT);
    }

    public static int[] solve(List<WynnItem> items, int[] stats) {
        int[] s = stats.clone();
        int[] result = solve(items.stream().map(WynnItem::requirements).map(a -> Arrays.stream(a).map(x -> x == 0 ? Integer.MIN_VALUE : x).toArray()).toArray(int[][]::new),
                     items.stream().map(WynnItem::bonuses).toArray(int[][]::new),
                     stats);
//        startStats = s;
//        System.out.println("Order: " + Arrays.toString(result));
//        printOrder(result);
        return result;
    }

    public static int[] solve(int[][] reqs, int[][] deltas, int[] stats) {
        OptimizedDFS.reqs = reqs;
        OptimizedDFS.deltas = deltas;
        startStats = stats;
        n = reqs.length;
        s = stats.length;
        List<Integer> prefix = new ArrayList<>();
        List<Integer> suffix = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < n; i++) remaining.add(i);
        collapseFree(remaining, prefix, stats);
        moveNegatives(remaining, suffix);
        int[] solution = runDFS(remaining, stats);
        return buildFullOrder(prefix, solution, suffix);
    }

    private static void collapseFree(List<Integer> remaining, List<Integer> prefix, int[] stats) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<Integer> it = remaining.iterator();
            while (it.hasNext()) {
                int i = it.next();
                if (isFree(i)) {
                    prefix.add(i);
                    for (int k = 0; k < s; k++) stats[k] += deltas[i][k];
                    it.remove();
                    changed = true;
                }
            }
        }
    }

    private static boolean isFree(int i) {
        for (int k = 0; k < s; k++) {
            if (Integer.MIN_VALUE < reqs[i][k]) return false;
            if (deltas[i][k] < 0) return false;
        }
        return true;
    }

    private static void moveNegatives(List<Integer> remaining, List<Integer> suffix) {
        Iterator<Integer> it = remaining.iterator();
        while (it.hasNext()) {
            int i = it.next();
            if (noReqs(i) && negativeDelta(i)) {
                suffix.add(0, i);
                it.remove();
            }
        }
    }

    private static boolean noReqs(int i) {
        for (int k = 0; k < s; k++) if (reqs[i][k] > Integer.MIN_VALUE) return false;
        return true;
    }

    private static boolean negativeDelta(int i) {
        for (int k = 0; k < s; k++) if (deltas[i][k] < 0) return true;
        return false;
    }

    private static boolean dominates(int a, int b) {
        for (int k = 0; k < s; k++) {
            if (reqs[a][k]   > reqs[b][k])   return false;
            if (deltas[a][k] < deltas[b][k]) return false;
        }

        boolean strictReq = false, strictDelta = false;
        for (int k = 0; k < s; k++) {
            if (reqs[a][k]   < reqs[b][k])   strictReq = true;
            if (deltas[a][k] > deltas[b][k]) strictDelta = true;
        }
        return strictReq && strictDelta;
    }

    // Note: 8 is the unique items. For current system, only 8 items are dealt with as tomes are added at the start and weapon reqs are checked after all SP are applied. If this changes, increment this to match new system
    private static final boolean[] seen = new boolean[1 << 8];

    private static int[] runDFS(List<Integer> remaining, int[] stats) {
        int m = remaining.size();
        int[] items = remaining.stream().mapToInt(x -> x).toArray();

        int[] edges = new int[n];
        long[] mustBeforeMask = new long[n];
        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                if (a != b && dominates(items[a], items[b])) {
                    edges[a]++;
                    mustBeforeMask[items[a]] |= (1L << b);
                }
            }
        }

        Integer[] boxed = Arrays.stream(items).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, (a, b) -> {
            if (edges[a] != edges[b]) return edges[b] - edges[a];
            return positiveDeltaSum(b) - positiveDeltaSum(a);
        });
        items = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();

        Arrays.fill(seen, false);

        boolean[] used = new boolean[n];
        int[] order = new int[m];
        int[] bestResult = new int[m];
        int[] bestOrderInfo = new int[]{0};
        int[] initialMaxRequirements = new int[s];
        Arrays.fill(initialMaxRequirements, Integer.MIN_VALUE);

        boolean foundFull = dfs(items, used, 0L, order, 0, stats.clone(), mustBeforeMask, bestResult, initialMaxRequirements, bestOrderInfo);

        if (foundFull)
            return bestResult;
        return Arrays.copyOf(bestResult, bestOrderInfo[0]);
    }

    private static boolean dfs(int[] items, boolean[] used, long usedMask, int[] order, int depth, int[] stats, long[] mustBeforeMask, int[] bestResult, int[] maxRequirements, int[] bestOrderInfo) {
        if (depth > bestOrderInfo[0]) {
            bestOrderInfo[0] = depth;
            System.arraycopy(order, 0, bestResult, 0, depth);
        }
        if (depth == items.length) return true;
        if (seen[(int)usedMask]) return false;
        for (int idx = 0; idx < items.length; idx++) {
            int item = items[idx];

            if (used[item]) continue;
            if ((mustBeforeMask[idx] & ~usedMask) != 0) continue;
            if (!canEquip(stats, reqs[item])) continue;

            used[item] = true;
            long newUsedMask = usedMask | (1L << idx);
            order[depth] = item;

            for (int k = 0; k < s; k++) stats[k] += deltas[item][k];

            int[] prevMaxReqs = new int[5];
            System.arraycopy(maxRequirements, 0, prevMaxReqs, 0, maxRequirements.length);
            for (int i = 0; i < reqs[item].length; i++) {
                maxRequirements[i] = Math.max(maxRequirements[i], reqs[item][i]);
            }

            boolean viable = canEquip(stats, maxRequirements);

            if (viable && dfs(items, used, newUsedMask, order, depth + 1, stats, mustBeforeMask, bestResult, maxRequirements, bestOrderInfo)) {
                revertState(item, stats);
                System.arraycopy(prevMaxReqs, 0, maxRequirements, 0, maxRequirements.length);
                used[item] = false;
                return true;
            }
            revertState(item, stats);
            System.arraycopy(prevMaxReqs, 0, maxRequirements, 0, maxRequirements.length);
            used[item] = false;
        }
        seen[(int)usedMask] = true;
        return false;
    }

    public static void revertState(int item, int[] stats) {
        for (int k = 0; k < s; k++) {
            stats[k] -= deltas[item][k];
        }
    }

    private static int positiveDeltaSum(int i) {
        int sum = 0;
        for (int k = 0; k < s; k++) if (deltas[i][k] > 0) sum += deltas[i][k];
        return sum;
    }

    private static boolean canEquip(int[] stats, int[] req) {
        int len = req.length;
        for (int k = 0; k < len; k++) if (stats[k] < req[k]) return false;
        return true;
    }
    private static int[] buildFullOrder(List<Integer> prefix, int[] middle, List<Integer> suffix) {
        int[] result = new int[prefix.size() + middle.length + suffix.size()];
        int idx = 0;
        for (int i : prefix)  result[idx++] = i;
        for (int i : middle)  result[idx++] = i;
        for (int i : suffix)  result[idx++] = i;
        return result;
    }

    private static void printOrder(int[] order) {
        int[] stats = startStats.clone();
        for (int idx : order) {
            System.out.printf("Item %2d | before: %s | req: %s%n",
                    idx, Arrays.toString(stats), Arrays.toString(reqs[idx]));
            for (int k = 0; k < s; k++) stats[k] += deltas[idx][k];
            System.out.printf("        | after:  %s%n", Arrays.toString(stats));
        }
    }
}