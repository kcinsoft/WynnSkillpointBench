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
        List<Named<SkillpointTest.TestCase>> cases = SkillpointTest.testCases().toList();

        // algo name -> per-case times (ns), accumulated across iterations
        Map<String, long[]> perCaseTotals = new LinkedHashMap<>();
        Map<String, Long> grandTotals = new LinkedHashMap<>();

        for (var algo : algos) {
            perCaseTotals.put(algo.getName(), new long[cases.size()]);
            grandTotals.put(algo.getName(), 0L);
        }

        // Warmup
        for (int w = 0; w < WARMUP_ITERATIONS; w++) {
            for (var algo : algos) {
                for (var tc : cases) {
                    algo.getPayload().check(tc.getPayload().items(), tc.getPayload().assignedSkillpoints().clone());
                }
            }
        }

        // Timed runs
        for (int i = 0; i < ITERATIONS; i++) {
            for (var algo : algos) {
                String name = algo.getName();
                long[] caseTotals = perCaseTotals.get(name);
                for (int c = 0; c < cases.size(); c++) {
                    SkillpointTest.TestCase tc = cases.get(c).getPayload();
                    long start = System.nanoTime();
                    algo.getPayload().check(tc.items(), tc.assignedSkillpoints().clone());
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
        System.out.println("  Summary (avg total across all cases)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        for (var algo : algos) {
            String name = algo.getName();
            double avgMs = grandTotals.get(name) / (double) ITERATIONS / 1e6;
            System.out.printf("  %-25s %8.3f ms%n", name, avgMs);
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

}
