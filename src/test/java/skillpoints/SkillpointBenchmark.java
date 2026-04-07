package skillpoints;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;

/**
 * Runs every test case against every algorithm {@code ITERATIONS} times
 * and prints average runtimes per algorithm.
 *
 * Run with: {@code ./gradlew test --tests "skillpoints.SkillpointBenchmark"}
 */
public class SkillpointBenchmark {

    private static final int WARMUP_ITERATIONS = 50;
    private static final int ITERATIONS = 1000;

    @Test
    void benchmarkAll() {
        List<Named<SkillpointChecker>> algos = SkillpointTest.algorithms().toList();
        List<Named<TestCases.TestCase>> cases = SkillpointTest.testCases().toList();

        // algo name -> per-case times (ns), accumulated across iterations
        Map<String, long[]> perCaseTotals = new LinkedHashMap<>();
        Map<String, Long> grandTotals = new LinkedHashMap<>();

        for (var algo : algos) {
            perCaseTotals.put(algo.getName(), new long[cases.size()]);
            grandTotals.put(algo.getName(), 0L);
        }

        Map<String, WynnItem[][]> benchmarkItems = new LinkedHashMap<>();
        Map<String, int[][]> benchmarkAssigned = new LinkedHashMap<>();
        for (var algo : algos) {
            String name = algo.getName();
            WynnItem[][] itemsByCase = new WynnItem[cases.size()][];
            int[][] assignedByCase = new int[cases.size()][];
            for (int c = 0; c < cases.size(); c++) {
                TestCases.TestCase testCase = cases.get(c).getPayload();
                itemsByCase[c] = SkillpointTest.cloneItems(testCase.items());
                assignedByCase[c] = testCase.assignedSkillpoints().clone();
            }
            benchmarkItems.put(name, itemsByCase);
            benchmarkAssigned.put(name, assignedByCase);
        }

        // Warmup
        for (int w = 0; w < WARMUP_ITERATIONS; w++) {
            for (var algo : algos) {
                String name = algo.getName();
                WynnItem[][] itemsByCase = benchmarkItems.get(name);
                int[][] assignedByCase = benchmarkAssigned.get(name);
                for (int c = 0; c < cases.size(); c++) {
                    runOnce(algo.getPayload(), itemsByCase, assignedByCase, c);
                }
            }
        }

        // Timed runs
        for (int i = 0; i < ITERATIONS; i++) {
            for (var algo : algos) {
                String name = algo.getName();
                long[] caseTotals = perCaseTotals.get(name);
                WynnItem[][] itemsByCase = benchmarkItems.get(name);
                int[][] assignedByCase = benchmarkAssigned.get(name);
                for (int c = 0; c < cases.size(); c++) {
                    long start = System.nanoTime();
                    runOnce(algo.getPayload(), itemsByCase, assignedByCase, c);
                    long elapsed = System.nanoTime() - start;
                    caseTotals[c] += elapsed;
                }
            }
        }

        // Print results
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Benchmark Results (%d iterations, %d warmup)%n", ITERATIONS, WARMUP_ITERATIONS);
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");

        // Per-case breakdown
        for (var algo : algos) {
            String name = algo.getName();
            long[] caseTotals = perCaseTotals.get(name);
            long grand = 0;
            System.out.printf("%n  %-25s%n", name);
            System.out.println("  ─────────────────────────────────────────────────────────────");
            for (int c = 0; c < cases.size(); c++) {
                double avgMs = caseTotals[c] / (double) ITERATIONS / 1e6;
                grand += caseTotals[c];
                System.out.printf("    %-45s avg: %8.3f ms%n", cases.get(c).getName(), avgMs);
            }
            grandTotals.put(name, grand);
            double grandAvgMs = grand / (double) ITERATIONS / 1e6;
            System.out.printf("    %-45s avg: %8.3f ms%n", "TOTAL", grandAvgMs);
        }

        // Summary table
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Summary (avg total across all cases, %d iterations, %d warmup)%n", ITERATIONS, WARMUP_ITERATIONS);
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        for (var algo : algos) {
            String name = algo.getName();
            double avgMs = grandTotals.get(name) / (double) ITERATIONS / 1e6;
            System.out.printf("  %-25s %8.3f ms%n", name, avgMs);
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    private static void runOnce(
            SkillpointChecker checker,
            WynnItem[][] itemsByCase,
            int[][] assignedByCase,
            int caseIndex) {
        if (checker instanceof GreedyAlgorithm) {
            checker.check(
                    SkillpointTest.cloneItems(itemsByCase[caseIndex]),
                    assignedByCase[caseIndex].clone());
            return;
        }
        checker.check(itemsByCase[caseIndex], assignedByCase[caseIndex]);
    }

}
